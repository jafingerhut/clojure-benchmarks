public final class JVMInfo {
    public static void showProp (String propName) {
	String propValue = System.getProperty(propName);
	if (propValue == null) {
	    System.out.println(propName + "=(null)");
	} else {
	    System.out.println(propName + "=" + propValue);
	}
    }

    public static void main (String[] args) {
	showProp("os.arch");
	Runtime rt = Runtime.getRuntime();
	if (rt != null) {
	    System.out.println("availableProcessors=" +
			       rt.availableProcessors());
	}
	showProp("os.name");
	showProp("os.version");
	showProp("java.runtime.version");
	showProp("sun.arch.data.model");
    }
}
