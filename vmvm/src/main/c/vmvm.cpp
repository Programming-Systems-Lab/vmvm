#include <stdio.h>
#include <stdlib.h>
#include <stddef.h>
#include <string.h>
#include "jvmti.h"
#include <time.h>

#include "jni.h"
#include "java_crw_demo.h"

#pragma GCC diagnostic ignored "-Wwrite-strings"
#pragma GCC diagnostic ignored "-Wmissing-declarations"

 #define PRINT_DEBUG
//#define PRINT_TIMING
typedef struct {
	/* JVMTI Environment */
	jvmtiEnv *jvmti;
	JNIEnv * jni;
	jboolean vm_is_started;
	jboolean vmDead;
	jmethodID intfcLookupMethod;
	jclass reintializerclass;
	/* Data access Lock */
	jrawMonitorID lock;
	JavaVM* jvm;
	int nClassesInstrumented;
} GlobalAgentData;

struct Field;
typedef struct Method {
	jmethodID methodID;
	char *name; //For debug only...

};
/**
 * We will need to cache information about each java class to be able to reference back to their fields later
 */
typedef struct Clazz {
	char *name;
	jclass clazz;
	jmethodID reinitMethod;
	Field **fields;
	int nFields;
	int class_data_len;
	bool ignoredClass;
	unsigned char * class_data;
	bool shouldInstrument;
	bool isInstrumentedAndWatched;
} Clazz;
typedef struct Field {
	Clazz *clazz;
	char *name;
	bool ignored;
	jfieldID fieldID;
};

static GlobalAgentData *gdata;

void fatal_error(const char * format, ...) {
	va_list ap;

	va_start(ap, format);
	(void) vfprintf(stderr, format, ap);
	(void) fflush(stderr);
	va_end(ap);
	exit(3);
}

static void check_jvmti_error(jvmtiEnv *jvmti, jvmtiError errnum,
		const char *str) {
	if (errnum != JVMTI_ERROR_NONE) {
		char *errnum_str;

		errnum_str = NULL;
		(void) jvmti->GetErrorName(errnum, &errnum_str);

		printf("ERROR: JVMTI: %d(%s): %s\n", errnum,
				(errnum_str == NULL ? "Unknown" : errnum_str),
				(str == NULL ? "" : str));
	}
}
/* Enter a critical section by doing a JVMTI Raw Monitor Enter */
static void enter_critical_section(jvmtiEnv *jvmti) {
	jvmtiError error;

	error = jvmti->RawMonitorEnter(gdata->lock);
	check_jvmti_error(jvmti, error, "Cannot enter with raw monitor");
}

/* Exit a critical section by doing a JVMTI Raw Monitor Exit */
static void exit_critical_section(jvmtiEnv *jvmti) {
	jvmtiError error;

	error = jvmti->RawMonitorExit(gdata->lock);
	check_jvmti_error(jvmti, error, "Cannot exit with raw monitor");
}
static void replaceClassMethodsWithReinit(Clazz * c) {
	c->shouldInstrument = true;
	gdata->jvmti->RetransformClasses(1, &(c->clazz));
}
/*
 * Update our cache of all of the loaded classes and their fields. We will need this information
 * to understand field relationships, which require calculating super classes, super interfaces, etc.
 */
static void updateClassCache(JNIEnv *env) {
	jvmtiError err;
	jint nClasses;
	jclass* classes;

	err = gdata->jvmti->GetLoadedClasses(&nClasses, &classes);
	check_jvmti_error(gdata->jvmti, err, "Cannot get classes");
//	if (classCache != NULL) {
//		free(classCache);
//	}
//	classCache = new Clazz*[nClasses];
//	memset(classCache, 0, sizeof(Clazz*) * nClasses);
//	sizeOfClassCache = nClasses;

	Clazz *c;
	jclass declaredOn;
	jlong declaredOnTag;
	int i;
	jlong tag;
	jint status;
	jfieldID *fields;
	int j;
	for (i = 0; i < nClasses; i++) {
		err = gdata->jvmti->GetTag(classes[i], &tag);
		check_jvmti_error(gdata->jvmti, err, "Unable to get class tag");
		if (!tag) {
			err = gdata->jvmti->GetClassStatus(classes[i], &status);
			check_jvmti_error(gdata->jvmti, err, "Cannot get class status");
			if ((status & JVMTI_CLASS_STATUS_PREPARED) == 0) {
//				classCache[i] = NULL;
				continue;
			}
			c = new Clazz();
			c->isInstrumentedAndWatched = false;
			c->ignoredClass = false;
			c->shouldInstrument = false;
			tag = (ptrdiff_t) (void*) c;
			c->clazz = (jclass) env->NewGlobalRef(classes[i]);
			gdata->jvmti->GetClassSignature(classes[i], &c->name, NULL);
			err = gdata->jvmti->GetClassFields(c->clazz, &(c->nFields),
					&fields);
			check_jvmti_error(gdata->jvmti, err, "Cannot get class fields");
			jint modifiers;
			err = gdata->jvmti->GetClassModifiers(c->clazz, &modifiers);
			c->fields = new Field*[c->nFields];
			for (j = 0; j < c->nFields; j++) {
				c->fields[j] = new Field();
				c->fields[j]->clazz = c;
				c->fields[j]->fieldID = fields[j];
				err = gdata->jvmti->GetFieldName(c->clazz, fields[j],
						&(c->fields[j]->name), NULL, NULL);
				check_jvmti_error(gdata->jvmti, err, "Can't get field name");
				if (strcmp("VMVM_RESET_IN_PROGRESS", c->fields[j]->name) == 0)
					c->fields[j]->ignored = 1;
				else
					c->fields[j]->ignored = 0;
			}
			gdata->jvmti->Deallocate((unsigned char *) (void*) fields);
			err = gdata->jvmti->SetTag(classes[i], (ptrdiff_t) (void*) c);
			check_jvmti_error(gdata->jvmti, err, "Cannot set class tag");

			if ((modifiers & 0x0200) != 0) {
				c->reinitMethod = NULL;
				c->ignoredClass = true;
				//It's an interface!
				jclass * intfcs;
				jint nIntfc;
				char * sig;
				int i;
				err = gdata->jvmti->GetImplementedInterfaces(c->clazz, &nIntfc,
						&intfcs);
				check_jvmti_error(gdata->jvmti, err, "Can't get intfcs");
				for (i = 0; i < nIntfc; i++) {
					err = gdata->jvmti->GetClassSignature(intfcs[i], &sig,
					NULL);
					check_jvmti_error(gdata->jvmti, err,
							"Can't get intfc name");
					if (strcmp(sig,
							"Ledu/columbia/cs/psl/vmvm/runtime/VMVMInstrumented;")
							== 0) {
						//This interface has been instrumented;
						int nameLen = strlen(c->name);
						char * newClass = new char[nameLen + 14];
						strcat(newClass, (c->name + 1));
						newClass[nameLen - 2] = '\0';
						strcat(newClass, "$$VMVMRESETTER");
						jclass resetterClass =
								(jclass) env->CallStaticObjectMethod(
										gdata->reintializerclass,
										gdata->intfcLookupMethod,
										env->NewStringUTF(newClass));
						if (resetterClass) {
							c->reinitMethod = env->GetStaticMethodID(
									resetterClass, "__vmvmReClinit", "(II)V");
							c->ignoredClass = false;
							if (c->reinitMethod == NULL) {
								//Not an instrumented class
								env->ExceptionClear();
								c->ignoredClass = true;
							}
						}
						break;
					}
					gdata->jvmti->Deallocate((unsigned char*) (void*) sig);
				}
				gdata->jvmti->Deallocate((unsigned char *) (void*) intfcs);

			} else {
				//TODO - this right now is going to pick up an ignored class that extends/implements an unignored!!
				//Cache the reinit method
				c->reinitMethod = env->GetStaticMethodID(c->clazz,
						"__vmvmReClinit", "(II)V");
				if (c->reinitMethod == NULL) {
					//Not an instrumented class
					env->ExceptionClear();
					c->ignoredClass = true;
				}
				else
				{
					//Make sure it's actually declared on this guy
					err = gdata->jvmti->GetMethodDeclaringClass(c->reinitMethod,&declaredOn);
					err = gdata->jvmti->GetTag(declaredOn, &declaredOnTag);

					if(declaredOnTag  != tag)
					{
						printf("Actually, going to ignore %s %lld,%lld\n",c->name,declaredOn,c->clazz);
						c->ignoredClass = true;
					}
				}
			}
		}
		c = (Clazz*) tag;
		if (!c->ignoredClass && !c->isInstrumentedAndWatched) {
#ifdef PRINT_DEBUG
			fprintf(stderr,"Watching %s\n",c->name);
#endif
			replaceClassMethodsWithReinit(c);
			for (j = 0; j < c->nFields; j++) {
				if (!c->fields[j]->ignored) {
					err = gdata->jvmti->SetFieldAccessWatch(c->clazz,
							c->fields[j]->fieldID);
					check_jvmti_error(gdata->jvmti, err,
							"Cannot set field watch");
					err = gdata->jvmti->SetFieldModificationWatch(c->clazz,
							c->fields[j]->fieldID);
					check_jvmti_error(gdata->jvmti, err,
							"Cannot set field watch");
				}
			}
			c->isInstrumentedAndWatched = true;
		}
	}
}
void JNICALL
cbClassFileLoadHook(jvmtiEnv *jvmti, JNIEnv* jni, jclass class_being_redefined,
		jobject loader, const char* name, jobject protection_domain,
		jint class_data_len, const unsigned char* class_data,
		jint* new_class_data_len, unsigned char** new_class_data) {
#ifdef PRINT_TIMING
	unsigned long startTime = time(NULL);
#endif
	if (!gdata->vmDead && class_being_redefined) {
		fprintf(stderr, "CBClassfileloadhook\n");

		enter_critical_section(jvmti);
#ifdef PRINT_DEBUG
		fprintf(stderr, "CBClassfileloadhookAcq\n");
#endif
		{
			jlong tag;
			Clazz *c;
			jvmtiError err;
			err = jvmti->GetTag(class_being_redefined, &tag);
			c = (Clazz*) tag;
			if (c && c->shouldInstrument) {
#ifdef PRINT_DEBUG
				printf("Instrumenting %s\n", c->name);
#endif
				c->shouldInstrument = false;

				//Save a copy of the current class definition
				c->class_data_len = class_data_len;
				jvmti->Allocate((jint) class_data_len, &c->class_data);
				(void) memcpy((void*) c->class_data, (void*) class_data,
						(int) class_data_len);

				*new_class_data_len = 0;
				*new_class_data = NULL;
				jint cnum;
				int systemClass;
				unsigned char *newImage;
				long newLength;

				systemClass = 0;
				if (!gdata->vm_is_started) {
					systemClass = 1;
				}
				newImage = NULL;
				newLength = 0;
				cnum = gdata->nClassesInstrumented;
				int nameLen = strlen(c->name);
				char classInternalName[nameLen - 2];
				int i;
				for (i = 0; i < nameLen - 2; i++) {
					classInternalName[i] = c->name[i + 1];
				}
				classInternalName[i] = '\0';

				java_crw_demo(cnum, name, class_data, class_data_len,
						systemClass, classInternalName, c->name,
						"__vmvmReClinit", "(II)V",
						NULL, NULL,
						NULL, NULL,
						NULL, NULL, &newImage, &newLength,
						NULL,
						NULL);
				if (newLength > 0) {
					unsigned char *jvmti_space;
					jvmti->Allocate((jint) newLength, &jvmti_space);
					(void) memcpy((void*) jvmti_space, (void*) newImage,
							(int) newLength);
					*new_class_data_len = (jint) newLength;
					*new_class_data = jvmti_space; /* VM will deallocate */
				}
				if (newImage != NULL) {
					(void) free((void*) newImage);
				}
				gdata->nClassesInstrumented++;

			} else if (c && c->class_data_len) {
#ifdef PRINT_DEBUG
				printf("Restoring %s\n",c->name);
#endif
				//Restore class to original state
				*new_class_data_len = (jint) c->class_data_len;
				*new_class_data = c->class_data;
			}
			else
			{
				printf("Unknown class\n");
			}
		}
		exit_critical_section(jvmti);
	}
#ifdef PRINT_TIMING
	fprintf(stderr,"ClassFileLoadHook: %u\n",(time(NULL)-startTime));
#endif
}
static void markAsDone(Clazz *c) {
	//Clear all field watches
#ifdef PRINT_DEBUG
	fprintf(stderr,"MAD %s\n", c->name);
#endif
	jvmtiError err;
	int i;
	for (i = 0; i < c->nFields; i++) {
		if (!c->fields[i]->ignored) {
			err = gdata->jvmti->ClearFieldAccessWatch(c->clazz,
					c->fields[i]->fieldID);
			check_jvmti_error(gdata->jvmti, err, "Can't clear field watch");
			err = gdata->jvmti->ClearFieldModificationWatch(c->clazz,
					c->fields[i]->fieldID);
			check_jvmti_error(gdata->jvmti, err, "Can't clear field watch");
		}
	}
	//Clear the method entry barriers
	err = gdata->jvmti->RetransformClasses(1, &c->clazz);
	check_jvmti_error(gdata->jvmti, err, "Can't modify class");
	c->isInstrumentedAndWatched = 0;
#ifdef PRINT_DEBUG
	fprintf(stderr,"END MAD %s\n", c->name);
#endif

}

static void reinitClassAndMarkAsDone(Clazz *c, JNIEnv *jni) {
	/*
	 * Calls the reinitializer directly, and then sets the flag to retransform the class, and remove
	 * any code in it that flags for reinit
	 */
#ifdef PRINT_DEBUG
	fprintf(stderr, "Reinit class and mark as done %s\n",c->name);
#endif
	jni->CallStaticVoidMethod(c->clazz, c->reinitMethod, NULL);
}
void JNICALL
cbFieldModification(jvmtiEnv *jvmti, JNIEnv* jni, jthread thread,
		jmethodID method, jlocation location, jclass field_klass,
		jobject object, jfieldID field, char signature_type, jvalue new_value) {
	jlong tag;
	jvmtiError err;
	err = jvmti->GetTag(field_klass, &tag);
	check_jvmti_error(jvmti, err, "Cant get class tag");
	if (tag) {
#ifdef PRINT_DEBUG
		fprintf(stderr, "CB field mod %s\n", ((Clazz*) tag)->name);
#endif
		reinitClassAndMarkAsDone((Clazz*) tag, jni);
	} else
		fprintf(stderr, "Unknown CB field mod\n");
}
void JNICALL
cbFieldAccess(jvmtiEnv *jvmti, JNIEnv* jni, jthread thread, jmethodID method,
		jlocation location, jclass field_klass, jobject object,
		jfieldID field) {
	jlong tag;
	jvmtiError err;
	err = jvmti->GetTag(field_klass, &tag);
	check_jvmti_error(jvmti, err, "Cant get class tag");
	if (tag) {
#ifdef PRINT_DEBUG
		fprintf(stderr, "CB field acc %s\n", ((Clazz*) tag)->name);
#endif
		reinitClassAndMarkAsDone((Clazz*) tag, jni);
	} else
		fprintf(stderr, "Unknown CB field acc\n");
}

JNIEXPORT static void JNICALL markAllClassesForReinit(JNIEnv *env,
		jclass klass) {
	if (gdata->vmDead) {
		return;
	}
	updateClassCache(env);
}
JNIEXPORT static void JNICALL reinitCalled(JNIEnv *env, jclass klass,
		jclass tgt) {
	if (gdata->vmDead) {
		return;
	}
	jlong tag;
	jvmtiError err;
	err = gdata->jvmti->GetTag(tgt, &tag);
	check_jvmti_error(gdata->jvmti, err, "Cant get class tag");
	if (tag) {
#ifdef PRINT_DEBUG
		fprintf(stderr, "Reinit called %s\n", ((Clazz*) tag)->name);
#endif
		markAsDone((Clazz*) tag);
	}
	else
		fprintf(stderr, "Unknown reinit called\n");
}

/*
 * Callback we receive when the JVM terminates - no more functions can be called after this
 */
static void JNICALL callbackVMDeath(jvmtiEnv * jvmti_env, JNIEnv * jni_env) {
	gdata->vmDead = JNI_TRUE;
}

/*
 * Callback we get when the JVM starts up, but before its initialized.
 * Sets up the JNI calls.
 */
static void JNICALL cbVMStart(jvmtiEnv * jvmti, JNIEnv * env) {

	enter_critical_section(jvmti);
	{
		jclass klass;
		jfieldID field;
		jint rc;
		static JNINativeMethod registry[2] = { { "_markAllClassesForReinit",
				"()V", (void*) &markAllClassesForReinit }, { "_reinitCalled",
				"(Ljava/lang/Class;)V", (void*) &reinitCalled } };
		/* Register Natives for class whose methods we use */
		klass = env->FindClass(
				"edu/columbia/cs/psl/vmvm/runtime/Reinitializer");
		if (klass == NULL) {
			fatal_error("ERROR: JNI: Cannot find JNI Helper with FindClass\n");
		}
		gdata->reintializerclass = (jclass) env->NewGlobalRef(klass);

		rc = env->RegisterNatives(klass, registry, 2);
		if (rc != 0) {
			fatal_error("ERROR: JNI: Cannot register natives\n");
		}
		/* Engage calls. */
		field = env->GetStaticFieldID(klass, "engaged", "I");
		if (field == NULL) {
			fatal_error("ERROR: JNI: Cannot get field\n");
		}
		env->SetStaticIntField(klass, field, 1);

		gdata->intfcLookupMethod = env->GetStaticMethodID(klass,
				"lookupInterfaceClass",
				"(Ljava/lang/String;)Ljava/lang/Class;");
		if (gdata->intfcLookupMethod == NULL) {
			fatal_error("ERROR: cannot get interface lookup method");
		}
		gdata->vm_is_started = JNI_TRUE;

	}
	exit_critical_section(jvmti);
}

/*
 * Callback that is notified when our agent is loaded. Registers for event
 * notifications.
 */
JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *jvm, char *options,
		void *reserved) {
	static GlobalAgentData data;
	jvmtiError error;
	jint res;
	jvmtiEventCallbacks callbacks;
	jvmtiEnv *jvmti = NULL;
	jvmtiCapabilities capa;

	(void) memset((void*) &data, 0, sizeof(data));
	gdata = &data;
//save jvmti for later
	gdata->jvm = jvm;
	res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_0);
	gdata->jvmti = jvmti;

	if (res != JNI_OK || jvmti == NULL) {
		/* This means that the VM was unable to obtain this version of the
		 *   JVMTI interface, this is a fatal error.
		 */
		printf("ERROR: Unable to access JVMTI Version 1 (0x%x),"
				" is your J2SE a 1.5 or newer version?"
				" JNIEnv's GetEnv() returned %d\n", JVMTI_VERSION_1, res);

	}

//Register our capabilities
	(void) memset(&capa, 0, sizeof(jvmtiCapabilities));
	capa.can_tag_objects = 1;
//	capa.can_redefine_any_class = 1;
	capa.can_generate_field_access_events = 1;
	capa.can_generate_field_modification_events = 1;
	capa.can_retransform_any_class = 1;
	capa.can_retransform_classes = 1;
	error = jvmti->AddCapabilities(&capa);
	check_jvmti_error(jvmti, error,
			"Unable to get necessary JVMTI capabilities.");

//Register callbacks
	(void) memset(&callbacks, 0, sizeof(callbacks));
	callbacks.VMDeath = &callbackVMDeath;
	callbacks.VMStart = &cbVMStart;
	callbacks.FieldAccess = &cbFieldAccess;
	callbacks.FieldModification = &cbFieldModification;
	callbacks.ClassFileLoadHook = &cbClassFileLoadHook;

	error = jvmti->SetEventCallbacks(&callbacks, (jint) sizeof(callbacks));
	check_jvmti_error(jvmti, error, "Cannot set jvmti callbacks");

//Register for events
	error = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_START,
			(jthread) NULL);
	check_jvmti_error(jvmti, error, "Cannot set event notification");
	error = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT,
			(jthread) NULL);
	check_jvmti_error(jvmti, error, "Cannot set event notification");
	error = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_DEATH,
			(jthread) NULL);
	check_jvmti_error(jvmti, error, "Cannot set event notification");
	error = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
			JVMTI_EVENT_FIELD_ACCESS, (jthread) NULL);
	check_jvmti_error(jvmti, error, "Cannot set event notification");
	error = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
			JVMTI_EVENT_FIELD_MODIFICATION, (jthread) NULL);
	check_jvmti_error(jvmti, error, "Cannot set event notification");
	error = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
			JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, (jthread) NULL);
	check_jvmti_error(jvmti, error, "Cannot set event notification");
//Set up a few locks
	error = jvmti->CreateRawMonitor("agent data", &(gdata->lock));
	check_jvmti_error(jvmti, error, "Cannot create raw monitor");

	return JNI_OK;
}
