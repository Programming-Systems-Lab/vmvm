package edu.columbia.cs.psl.vmvm;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import edu.columbia.cs.psl.vmvm.struct.MethodInvocation;


public abstract class AbstractLazyCloningInterceptor extends AbstractInterceptor {

	public AbstractLazyCloningInterceptor(Object intercepted) {
		super(intercepted);
	}
	public static Object getRootCallee()
	{
		return createdCallees.get(getThreadChildId());
	}
	private static HashMap<Integer, Object> createdCallees = new HashMap<Integer, Object>();
	protected Thread createRunnerThread(final MethodInvocation inv, boolean isChild)
	{
			final int id = (isChild ? nextId.getAndIncrement() : 0);
		return new Thread(new ThreadGroup(THREAD_PREFIX+id),new Runnable() {		
			
			public void run() {
				try {
					createdCallees.put(id, inv.getCallee());
					if(id > 0)
					setAsChild(inv.getCallee(),id);
					if (inv.getMethod() == null)
						throw new NullPointerException();
					System.out.println("Calling " + inv.getMethod());
					inv.setReturnValue(inv.getMethod().invoke(inv.getCallee(), inv.getParams()));
					cleanupChild(id);
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				} catch (NullPointerException e) {
					e.printStackTrace();
				}
			}
		},THREAD_PREFIX+id);
	}
	protected Thread createChildThread(MethodInvocation inv)
	{
		return createRunnerThread(inv, true);
	}

	@Override
	protected void cleanupChild(int childId) {
		super.cleanupChild(childId);
		createdCallees.remove(childId);
	}
	
	
}
