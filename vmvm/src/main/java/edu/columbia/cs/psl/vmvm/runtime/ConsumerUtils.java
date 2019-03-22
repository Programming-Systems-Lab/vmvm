package edu.columbia.cs.psl.vmvm.runtime;

import edu.columbia.cs.psl.vmvm.runtime.inst.Utils;

public class ConsumerUtils {
	public boolean isIgnoredClass(String internalName) {
		return Utils.ignorePattern != null && internalName.startsWith(Utils.ignorePattern);
	}
}
