package io.quarkus.commons.classloading;

public final class ClassLoaderHelper {

    private static final String JAVA = "java.";
    private static final String JDK_INTERNAL = "jdk.internal.";
    private static final String SUN_MISC = "sun.misc.";

    private ClassLoaderHelper() {
        //Not meant to be instantiated
    }

    /**
     * Helper method to convert a classname into its typical resource name:
     * replace all "." with "/" and append the ".class" postfix.
     *
     * @param className
     * @return the name of the respective resource
     */
    public static String fromClassNameToResourceName(final String className) {
        //Important: avoid indy!
        return className.replace('.', '/').concat(".class");
    }

    public static boolean isInJdkPackage(String name) {
        return name.startsWith(JAVA) || name.startsWith(JDK_INTERNAL) || name.startsWith(SUN_MISC);
    }
}
