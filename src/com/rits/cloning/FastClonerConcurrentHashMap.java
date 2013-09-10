package com.rits.cloning;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author kostantinos.kougios
 *
 * 18 Oct 2011
 */
public class FastClonerConcurrentHashMap implements IFastCloner
{
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Object clone(final Object t, final Cloner cloner, final Map<Object, Object> clones,int sandboxField) throws IllegalAccessException
	{
		synchronized (t) {
			
		final ConcurrentHashMap<Object, Object> m = (ConcurrentHashMap) t;
		final ConcurrentHashMap result = new ConcurrentHashMap();
		synchronized (m.entrySet()) {
			
		for (final Map.Entry e : m.entrySet())
		{
			synchronized (e.getKey()) {
				
			
			final Object key = cloner.cloneInternal(e.getKey(), clones, sandboxField);
			final Object value = cloner.cloneInternal(e.getValue(), clones, sandboxField);
			if(key != null && value != null)
			result.put(key, value);
			}
		}
		
		return result;
		}
		}
	}
}
