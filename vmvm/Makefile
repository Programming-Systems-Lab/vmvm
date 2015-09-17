UNAME := $(shell uname)
CCFLAGS =  -I${JAVA_HOME}/include -c -fPIC -fpermissive
ifeq ($(UNAME), Linux)
	CCFLAGS += -I${JAVA_HOME}/include/linux 
	LINKFLAGS = -z defs -static-libgcc -shared -o target/libvmvm.so -lc 
endif
ifeq ($(UNAME), Darwin)
	CCFLAGS += -I${JAVA_HOME}/include/darwin
	LINKFLAGS += -dynamiclib -o target/libvmvm.so
endif

libvmvm.so:
	gcc ${CCFLAGS} -o target/java_crw_demo.o src/main/c/java_crw_demo.c
	gcc ${CCFLAGS} -o target/libvmvm.o src/main/c/vmvm.cpp 
	g++ ${LINKFLAGS} target/libvmvm.o target/java_crw_demo.o
clean:
	rm -rf target/libvmvm.o target/java_crw_demo.o target/libvmvm.so
