package edu.columbia.cs.psl.vmvm.runtime;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;


public final class Reinitializer {
	public static final String INTERNAL_NAME = "edu/columbia/cs/psl/vmvm/runtime/Reinitializer";

	public static Instrumentation inst;

	static LinkedList<WeakReference<ClassState>> classesToReinit = new LinkedList<WeakReference<ClassState>>();
	public static HashSet<String> classesNotYetReinitialized = new HashSet<String>();

	static HashMap<String, WeakReference<ClassState>> initializedClasses = new HashMap<String, WeakReference<ClassState>>();

	public static final void logMessage(String s) {
		System.err.println(s + ": " + Thread.currentThread().getName());
	}

	/**
	 * Called on interfaces
	 * 
	 * @param clazz
	 * @param fieldName
	 * @return
	 */
	public static final boolean _vmvmReinitFieldCheck(String clazz, String fieldName) {
		if (initializedClasses.get(clazz) == null)
			return false;
		ClassState cs = initializedClasses.get(clazz).get();
		if (cs == null)
			return false;
		return tryToReinitForFieldOnInterface(cs, fieldName);
	}

	public static final boolean tryToReinitForFieldOnInterface(ClassState cs, String fieldName) {
		tryToReinitForField(cs, fieldName, true);
		return false;
	}
	private static final boolean tryToReinitForField(ClassState cs, String fieldName, boolean lookOnThisClass) {
		if(!cs.fullyPopulated)
			cs.populate(initializedClasses);
		if(lookOnThisClass)
		{
			for(Field f : cs.fields)
			{
				if(f.getName().equals(fieldName))
				{
					try {
						if(cs.isInterface)
							callReinitOnInterface(cs.name+"$$VMVM_RESETTER");
						else
						cs.clazz.getDeclaredMethod("__vmvmReClinit", null).invoke(null);
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
						e.printStackTrace();
					}
					return true;
				}
			}
		}
		//First, look on parent
		if(cs.parent != null && tryToReinitForField(cs.parent, fieldName, true))
			return true;
		//Now, look on all ifaces
		for(ClassState _cs : cs.interfaces)
		{
			if(_cs != null)
				if(tryToReinitForField(_cs, fieldName, true))
					return true;
		}
		return false;
	}
		
	public static final void tryToReinitForField(ClassState cs, String fieldName) {
		tryToReinitForField(cs, fieldName, false);
	}

	public static final void callReinitOnInterface(String c) {
		if(VMVMClassFileTransformer.DEBUG)
			System.out.println("Reinit on intfc" + c);
		try {
			Class cl = lookupInterfaceClass(c);
			cl.getDeclaredMethod("__vmvmReClinit", null).invoke(null);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		}

	}

	public static synchronized final void markAllClassesForReinit() {
		//		long start = System.currentTimeMillis();
		//		System.err.println("Start MCR ");
		//		ClassDefinition[] toReinit = new ClassDefinition[classesToReinit.size()];
		resetInternalStatics();

		synchronized (shutdownHooks) {
			for (Thread t : shutdownHooks) {
				Runtime.getRuntime().removeShutdownHook(t);
			}
			shutdownHooks.clear();
		}
		try {
			ArrayList<MBeanServer> mbeanServers = MBeanServerFactory.findMBeanServer(null);
			if (mbeanServers != null && mbeanServers.size() > 0) {
				for (MBeanServer server : mbeanServers) {
					//				System.out.println("Releasing server " + server);
					MBeanServerFactory.releaseMBeanServer(server);
				}
			}
			mbeanServers = null;
			MBeanServer server = ManagementFactory.getPlatformMBeanServer();
			if (server.getMBeanCount() > 0) {
				Set<ObjectName> onames = server.queryNames(null, null);
				for (ObjectName name : onames)
					try {
						server.unregisterMBean(name);
					} catch (Throwable e) {
					}
			}
			server = null;
		} catch (Throwable t) {
			//nop
		}

		for (String s : properties.keySet()) {
			//							System.out.println("Reseting " + s + " from <" + System.getProperty(s) + "> to <" + properties.get(s) + ">");
			if (properties.get(s) != null)
				System.setProperty(s, properties.get(s));
			else
				System.clearProperty(s);
		}
		properties.clear();
		LinkedList<WeakReference<ClassState>> toReinit = classesToReinit;
		classesToReinit = new LinkedList<WeakReference<ClassState>>();
		classesNotYetReinitialized.clear();
		for (WeakReference<ClassState> w : toReinit) {
			if (w.get() != null) {
				ClassState c = w.get();
				classesNotYetReinitialized.add(c.name);
				try {
					c.needsReinit = true;
					c.hasClassesToOptAway = false;
//					if (VMVMClassFileTransformer.DEBUG) {
//						System.out.println("MCR " + c.name);
//						File debugDir = new File("debug-readin");
//						if (!debugDir.exists())
//							debugDir.mkdir();
//						try {
//							File fi = new File("debug-readin/" + c.name.replace("/", ".") + ".class");
//							FileOutputStream fos = new FileOutputStream(fi);
//							fos.write(c.fullyInstrumentedClass);
//							fos.close();
//						} catch (Throwable t) {
//							t.printStackTrace();
//						}
//					}
					if (VMVMClassFileTransformer.ALWAYS_REOPT && c.isOptimized)
						inst.redefineClasses(new ClassDefinition(c.clazz, c.fullyInstrumentedClass));
					//					System.err.println("Adding checks in " + c);
				} catch (IllegalArgumentException | NoClassDefFoundError | ClassNotFoundException | UnmodifiableClassException ex) {
					ex.printStackTrace();
				}

			}
		}
		Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());

		//		System.err.println("End MCR " + (System.currentTimeMillis() - start));
	}

	private static HashSet<Thread> shutdownHooks = new HashSet<Thread>();

	public static void addShutdownHook(Runtime r, Thread t) {
		synchronized (shutdownHooks) {
			r.addShutdownHook(t);
			shutdownHooks.add(t);
		}
	}

	public static final void fixEnum(Class<?> c) {
		if (VMVMClassFileTransformer.DEBUG)
			System.out.println("Fixup enum " + c);
		try {
			Field f = Class.class.getDeclaredField("enumConstants");
			f.setAccessible(true);
			f.set(c, null);
			f = Class.class.getDeclaredField("enumConstantDirectory");
			f.setAccessible(true);
			f.set(c, null);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public static final void reopt(Class<?> c) {
		try {
			System.err.println("Reoptimizing " + c);
			synchronized (c) {
				inst.retransformClasses(c);
				c.notifyAll();
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public static final void reinitCalled(ClassState c) {
//		if (VMVMClassFileTransformer.DEBUG)
//			System.out.println("REinit called " + c.name + " in " + Thread.currentThread().getName());
		//		if(c.getName().contains("BaseTest"))
		//			new Exception().printStackTrace();
		classesNotYetReinitialized.remove(c.name);
		classesToReinit.add(new WeakReference<ClassState>(c));
		//		inst.redefineClasses(definitions);
	}

	public static final void clinitCalled(ClassState c) {
//		if (VMVMClassFileTransformer.DEBUG)
//			System.out.println("CLinit called " + c.clazz + " in " + Thread.currentThread().getName());
		//				if(c.getName().contains("BaseTest"))
		//					new Exception().printStackTrace();
		initializedClasses.put(c.name, new WeakReference<ClassState>(c));
		classesToReinit.add(new WeakReference<ClassState>(c));
//		try {
//			byte[] uninst = VMVMClassFileTransformer.instrumentedClasses.remove(c.name.replace(".", "/"));
//			c.originalClass = uninst;
//			ClassReader cr = new ClassReader(uninst);
//			ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
//			ReinitCheckForceCV cv = new ReinitCheckForceCV(cw, false);
//			try {
//				cr.accept(cv, ClassReader.EXPAND_FRAMES);
//			} catch (Throwable t) {
//				throw new IllegalStateException(t);
//			}
//			c.fullyInstrumentedClass = cw.toByteArray();
//			if (VMVMClassFileTransformer.DEBUG) {
//				File debugDir = new File("debug-instheavy");
//				if (!debugDir.exists())
//					debugDir.mkdir();
//				File f = new File("debug-instheavy/" + c.name.replace("/", ".") + ".class");
//				FileOutputStream fos = new FileOutputStream(f);
//				fos.write(c.fullyInstrumentedClass);
//				fos.close();
//			}
//
//		} catch (IllegalArgumentException | SecurityException | IOException e) {
//			e.printStackTrace();
//		}
	}

	public static Class<?> lookupInterfaceClass(String name) {
		try {
			Class ret = VMVMClassFileTransformer.cl.loadClass(name.replace("/", "."));
			if (ret == null)
				throw new IllegalArgumentException("Cant find interface resetter for " + name);
			return ret;
		} catch (Throwable t) {
			t.printStackTrace();
			return null;
		}
	}

	private static Field URLStreamHandlerField;

	private static void resetInternalStatics() {
		if (URLStreamHandlerField == null) {
			try {
				URLStreamHandlerField = URL.class.getDeclaredField("factory");
				URLStreamHandlerField.setAccessible(true);
			} catch (NoSuchFieldException e) {
			} catch (SecurityException e) {
			}
		}
		try {
			URLStreamHandlerField.set(null, null);
		} catch (Throwable t) {
		}

		if (logsUsed[44]) {
			try {
				javax.security.auth.Policy.setPolicy((javax.security.auth.Policy) loggedValues[44]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[44] = false;
			loggedValues[44] = null;
		}
		if (logsUsed[13]) {
			try {
				java.lang.Thread.setDefaultUncaughtExceptionHandler((java.lang.Thread.UncaughtExceptionHandler) loggedValues[13]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[13] = false;
			loggedValues[13] = null;
		}
		if (logsUsed[17]) {
			try {
				java.net.URLConnection.setContentHandlerFactory((java.net.ContentHandlerFactory) loggedValues[17]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[17] = false;
			loggedValues[17] = null;
		}
		if (logsUsed[18]) {
			try {
				java.net.URLConnection.setFileNameMap((java.net.FileNameMap) loggedValues[18]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[18] = false;
			loggedValues[18] = null;
		}
		if (logsUsed[19]) {
			try {
				java.net.URLConnection.setDefaultAllowUserInteraction((boolean) loggedValues[19]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[19] = false;
			loggedValues[19] = null;
		}
		if (logsUsed[20]) {//try{
			//java.net.URLConnection.setDefaultRequestProperty(()loggedValues[20]);}catch(Exception ex){ex.printStackTrace();}
			logsUsed[20] = false;
			loggedValues[20] = null;
		}
		if (logsUsed[16]) {
			try {
				java.net.ResponseCache.setDefault((java.net.ResponseCache) loggedValues[16]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[16] = false;
			loggedValues[16] = null;
		}
		if (logsUsed[37]) {
			try {
				javax.imageio.ImageIO.setUseCache((boolean) loggedValues[37]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[37] = false;
			loggedValues[37] = null;
		}
		if (logsUsed[38]) {
			try {
				javax.imageio.ImageIO.setCacheDirectory((java.io.File) loggedValues[38]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[38] = false;
			loggedValues[38] = null;
		}
		if (logsUsed[35]) {
			try {
				javax.activation.CommandMap.setDefaultCommandMap((javax.activation.CommandMap) loggedValues[35]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[35] = false;
			loggedValues[35] = null;
		}
		if (logsUsed[63]) {
			try {
				javax.swing.plaf.synth.SynthLookAndFeel.setStyleFactory((javax.swing.plaf.synth.SynthStyleFactory) loggedValues[63]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[63] = false;
			loggedValues[63] = null;
		}
		if (logsUsed[55]) {
			try {
				javax.swing.Timer.setLogTimers((boolean) loggedValues[55]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[55] = false;
			loggedValues[55] = null;
		}
		if (logsUsed[43]) {
			try {
				javax.net.ssl.SSLContext.setDefault((javax.net.ssl.SSLContext) loggedValues[43]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[43] = false;
			loggedValues[43] = null;
		}
		if (logsUsed[21]) {
			try {
				java.rmi.activation.ActivationGroup.setSystem((java.rmi.activation.ActivationSystem) loggedValues[21]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[21] = false;
			loggedValues[21] = null;
		}
		if (logsUsed[41]) {
			try {
				javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory((javax.net.ssl.SSLSocketFactory) loggedValues[41]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[41] = false;
			loggedValues[41] = null;
		}
		if (logsUsed[42]) {
			try {
				javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier((javax.net.ssl.HostnameVerifier) loggedValues[42]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[42] = false;
			loggedValues[42] = null;
		}
		if (logsUsed[54]) {
			try {
				javax.swing.PopupFactory.setSharedInstance((javax.swing.PopupFactory) loggedValues[54]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[54] = false;
			loggedValues[54] = null;
		}
		if (logsUsed[51]) {
			try {
				javax.swing.JPopupMenu.setDefaultLightWeightPopupEnabled((boolean) loggedValues[51]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[51] = false;
			loggedValues[51] = null;
		}
		if (logsUsed[14]) {
			try {
				java.net.CookieHandler.setDefault((java.net.CookieHandler) loggedValues[14]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[14] = false;
			loggedValues[14] = null;
		}
		if (logsUsed[46]) {
			try {
				javax.sql.rowset.spi.SyncFactory.setLogger((java.util.logging.Logger) loggedValues[46]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[46] = false;
			loggedValues[46] = null;
		}
		if (logsUsed[47]) {
			try {
				javax.sql.rowset.spi.SyncFactory.setJNDIContext((javax.naming.Context) loggedValues[47]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[47] = false;
			loggedValues[47] = null;
		}
		if (logsUsed[67]) {
			try {
				java.net.Authenticator.setDefault((java.net.Authenticator) loggedValues[67]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[67] = false;
			loggedValues[67] = null;
		}
		if (logsUsed[53]) {
			try {
				javax.swing.LayoutStyle.setInstance((javax.swing.LayoutStyle) loggedValues[53]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[53] = false;
			loggedValues[53] = null;
		}
		if (logsUsed[39]) {
			try {
				javax.naming.spi.NamingManager.setInitialContextFactoryBuilder((javax.naming.spi.InitialContextFactoryBuilder) loggedValues[39]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[39] = false;
			loggedValues[39] = null;
		}
		if (logsUsed[40]) {
			try {
				javax.naming.spi.NamingManager.setObjectFactoryBuilder((javax.naming.spi.ObjectFactoryBuilder) loggedValues[40]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[40] = false;
			loggedValues[40] = null;
		}
		if (logsUsed[45]) {
			try {
				javax.security.auth.login.Configuration.setConfiguration((javax.security.auth.login.Configuration) loggedValues[45]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[45] = false;
			loggedValues[45] = null;
		}
		if (logsUsed[65]) {
			try {
				javax.swing.JDialog.setDefaultLookAndFeelDecorated((boolean) loggedValues[65]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[65] = false;
			loggedValues[65] = null;
		}
		if (logsUsed[30]) {
			try {
				java.sql.DriverManager.setLoginTimeout((int) loggedValues[30]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[30] = false;
			loggedValues[30] = null;
		}
		if (logsUsed[32]) {
			try {
				java.sql.DriverManager.setLogWriter((java.io.PrintWriter) loggedValues[32]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[32] = false;
			loggedValues[32] = null;
		}
		if (logsUsed[31]) {
			try {
				java.sql.DriverManager.setLogStream((java.io.PrintStream) loggedValues[31]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[31] = false;
			loggedValues[31] = null;
		}
		if (logsUsed[50]) {
			try {
				javax.swing.JOptionPane.setRootFrame((java.awt.Frame) loggedValues[50]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[50] = false;
			loggedValues[50] = null;
		}
		if (logsUsed[49]) {
			try {
				javax.swing.JComponent.setDefaultLocale((java.util.Locale) loggedValues[49]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[49] = false;
			loggedValues[49] = null;
		}
		if (logsUsed[3]) {//try{
			//java.awt.AWTEventMulticaster..(()loggedValues[3]);}catch(Exception ex){ex.printStackTrace();}
			logsUsed[3] = false;
			loggedValues[3] = null;
		}
		if (logsUsed[15]) {
			try {
				java.net.ProxySelector.setDefault((java.net.ProxySelector) loggedValues[15]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[15] = false;
			loggedValues[15] = null;
		}
		if (logsUsed[5]) {
			try {
				java.beans.Introspector.setBeanInfoSearchPath((java.lang.String[]) loggedValues[5]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[5] = false;
			loggedValues[5] = null;
		}
		if (logsUsed[36]) {
			try {
				javax.activation.FileTypeMap.setDefaultFileTypeMap((javax.activation.FileTypeMap) loggedValues[36]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[36] = false;
			loggedValues[36] = null;
		}
		if (logsUsed[29]) {//try{
			//java.security.Security.setProperty(()loggedValues[29]);}catch(Exception ex){ex.printStackTrace();}
			logsUsed[29] = false;
			loggedValues[29] = null;
		}
		if (logsUsed[4]) {
			try {
				java.awt.KeyboardFocusManager.setCurrentKeyboardFocusManager((java.awt.KeyboardFocusManager) loggedValues[4]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[4] = false;
			loggedValues[4] = null;
		}
		if (logsUsed[26]) {
			try {
				java.security.Policy.setPolicy((java.security.Policy) loggedValues[26]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[26] = false;
			loggedValues[26] = null;
		}
		if (logsUsed[23]) {
			try {
				java.rmi.server.RMISocketFactory.setFailureHandler((java.rmi.server.RMIFailureHandler) loggedValues[23]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[23] = false;
			loggedValues[23] = null;
		}
		if (logsUsed[24]) {
			try {
				java.rmi.server.RMISocketFactory.setSocketFactory((java.rmi.server.RMISocketFactory) loggedValues[24]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[24] = false;
			loggedValues[24] = null;
		}
		if (logsUsed[64]) {
			try {
				javax.swing.text.LayoutQueue.setDefaultQueue((javax.swing.text.LayoutQueue) loggedValues[64]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[64] = false;
			loggedValues[64] = null;
		}
		if (logsUsed[33]) {
			try {
				java.util.Locale.setDefault((java.util.Locale) loggedValues[33]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[33] = false;
			loggedValues[33] = null;
		}
		if (logsUsed[66]) {
			try {
				javax.swing.JFrame.setDefaultLookAndFeelDecorated((boolean) loggedValues[66]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[66] = false;
			loggedValues[66] = null;
		}
		if (logsUsed[7]) {
			try {
				java.lang.System.setOut((java.io.PrintStream) loggedValues[7]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[7] = false;
			loggedValues[7] = null;
		}
		if (logsUsed[8]) {
			try {
				java.lang.System.setIn((java.io.InputStream) loggedValues[8]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[8] = false;
			loggedValues[8] = null;
		}
		if (logsUsed[9]) {
			try {
				java.lang.System.setProperties((java.util.Properties) loggedValues[9]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[9] = false;
			loggedValues[9] = null;
		}
		if (logsUsed[10]) {
			try {
				java.lang.System.setSecurityManager((java.lang.SecurityManager) loggedValues[10]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[10] = false;
			loggedValues[10] = null;
		}
		if (logsUsed[11]) {//try{
			//java.lang.System.setProperty(()loggedValues[11]);}catch(Exception ex){ex.printStackTrace();}
			logsUsed[11] = false;
			loggedValues[11] = null;
		}
		if (logsUsed[12]) {
			try {
				java.lang.System.setErr((java.io.PrintStream) loggedValues[12]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[12] = false;
			loggedValues[12] = null;
		}
		if (logsUsed[6]) {
			try {
				java.beans.PropertyEditorManager.setEditorSearchPath((java.lang.String[]) loggedValues[6]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[6] = false;
			loggedValues[6] = null;
		}
		if (logsUsed[62]) {//try{
			//javax.swing.plaf.nimbus.EffectUtils.setPixels(()loggedValues[62]);}catch(Exception ex){ex.printStackTrace();}
			logsUsed[62] = false;
			loggedValues[62] = null;
		}
		if (logsUsed[34]) {
			try {
				java.util.TimeZone.setDefault((java.util.TimeZone) loggedValues[34]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[34] = false;
			loggedValues[34] = null;
		}
		if (logsUsed[61]) {
			try {
				javax.swing.UIManager.setInstalledLookAndFeels((javax.swing.UIManager.LookAndFeelInfo[]) loggedValues[61]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[61] = false;
			loggedValues[61] = null;
		}
		if (logsUsed[60]) {
			try {
				javax.swing.UIManager.setLookAndFeel((java.lang.String) loggedValues[60]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[60] = false;
			loggedValues[60] = null;
		}
		if (logsUsed[52]) {//try{
			//javax.swing.KeyboardManager.setCurrentManager((javax.swing.KeyboardManager)loggedValues[52]);}catch(Exception ex){ex.printStackTrace();}
			logsUsed[52] = false;
			loggedValues[52] = null;
		}
		if (logsUsed[48]) {
			try {
				javax.swing.FocusManager.setCurrentManager((javax.swing.FocusManager) loggedValues[48]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[48] = false;
			loggedValues[48] = null;
		}
		if (logsUsed[22]) {
			try {
				java.rmi.server.LogStream.setDefaultStream((java.io.PrintStream) loggedValues[22]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[22] = false;
			loggedValues[22] = null;
		}
		if (logsUsed[25]) {
			try {
				java.rmi.server.RemoteServer.setLog((java.io.OutputStream) loggedValues[25]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logsUsed[25] = false;
			loggedValues[25] = null;
		}

		if (logsUsed[1]) {
			loggedValues[1] = null;
			logsUsed[1] = false;
		}
		if (logsUsed[2]) {
			loggedValues[2] = null;
			logsUsed[2] = false;
		}
		if (logsUsed[27]) {
			loggedValues[27] = null;
			logsUsed[27] = false;
		}
		if (logsUsed[28]) {
			loggedValues[28] = null;
			logsUsed[28] = false;
		}
		if (logsUsed[56]) {
			for (WeakReference<Object> o : (LinkedList<WeakReference<Object>>) loggedValues[56])
				if (!o.isEnqueued())
					javax.swing.UIManager.removePropertyChangeListener((java.beans.PropertyChangeListener) o.get());
			loggedValues[56] = null;
			logsUsed[56] = false;
		}
		if (logsUsed[57]) {
			for (WeakReference<Object> o : (LinkedList<WeakReference<Object>>) loggedValues[57])
				if (!o.isEnqueued())
					javax.swing.UIManager.removeAuxiliaryLookAndFeel((javax.swing.LookAndFeel) o.get());
			loggedValues[57] = null;
			logsUsed[57] = false;
		}
		if (logsUsed[58]) {
			for (Object o : (LinkedList<Object>) loggedValues[58])
				javax.swing.UIManager.addPropertyChangeListener((java.beans.PropertyChangeListener) o);
			loggedValues[58] = null;
			logsUsed[58] = false;
		}
		if (logsUsed[59]) {
			for (Object o : (LinkedList<Object>) loggedValues[59])
				javax.swing.UIManager.addAuxiliaryLookAndFeel((javax.swing.LookAndFeel) o);
			loggedValues[59] = null;
			logsUsed[59] = false;
		}

	}

	public static void logStaticInternal(Object o, int i) {
		if (logsUsed[i])
			return;
		logsUsed[i] = true;
		loggedValues[i] = o;
	}

	@SuppressWarnings("unchecked")
	public static void logStaticInternalAdd(Object o, int i) {
		if (!logsUsed[i])
			loggedValues[i] = new LinkedList<WeakReference<Object>>();
		logsUsed[i] = true;
		((LinkedList<WeakReference<Object>>) loggedValues[i]).add(new WeakReference<Object>(o));
	}

	public static void logStaticInternalRemove(Object o, int i) {
		if (!logsUsed[i])
			loggedValues[i] = new LinkedList<Object>();
		logsUsed[i] = true;
		((LinkedList<Object>) loggedValues[i]).add(o);
	}

	public static String logAndSetProperty(String key, String value) {
		if (!properties.containsKey(key))
			properties.put(key, System.getProperty(key));
		return System.setProperty(key, value);
	}

	public static String logAndGetProperty(String key) {
		String ret = System.getProperty(key);
		//System.out.println("SYs prop " + key+"="+ret);
		return ret;
	}

	public static String logAndGetProperty(String key, String def) {
		String ret = System.getProperty(key, def);
		//System.out.println("SYs prop " + key+"="+ret +", def was " + def);
		return ret;
	}

	public static void logAndSetProperty(Properties values) {
		for (Object key : values.keySet()) {
			if (!properties.containsKey(key))
				properties.put((String) key, System.getProperty((String) key));
		}
		System.setProperties(values);
	}

	public static Object[] loggedValues = new Object[74];
	public static boolean[] logsUsed = new boolean[74];
	public static HashMap<String, String> properties = new HashMap<>();
}
