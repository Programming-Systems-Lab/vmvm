package edu.columbia.cs.psl.vmvm;

public interface Constants {
	public static String BEEN_CLONED_FIELD = "_vmvm_acc_logged";
	public static String CHILD_FIELD = "_invivo_cloned";
	public static String THREAD_PREFIX = "_invivo_child";
	public static String SANDBOX_SUFFIX = "_vmvm_";
	public static int MAX_CHILDREN = 1;
	public static String CHROOT_DIR = "/private/tmp/chroot";
	public static String VMVM_STATIC_RESET_METHOD = "vmvm_reinit_statics";
	public static String VMVM_NEEDS_RESET = "vmvm_needs_reset";
	public static String VMVM_RESET_IN_PROGRESS = "vmvm_reset_in_progress";

	public static String VMVM_STATIC_RESET_METHOD_WITH_CHECK = "vmvm_reinit_statics_with_check";

}
