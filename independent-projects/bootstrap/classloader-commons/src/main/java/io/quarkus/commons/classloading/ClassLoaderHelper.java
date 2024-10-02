package io.quarkus.commons.classloading;

public final class ClassLoaderHelper {

    private static final String JAVA = "java.";
    private static final String JDK_INTERNAL = "jdk.internal.";
    private static final String SUN_MISC = "sun.misc.";

    private static final String CLASS_SUFFIX = ".class";

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
        return className.replace('.', '/').concat(CLASS_SUFFIX);
    }

    /**
     * Helper method to convert a resource name into the corresponding class name:
     * replace all "/" with "." and remove the ".class" postfix.
     *
     * @param resourceName
     * @return the name of the respective class
     */
    public static String fromResourceNameToClassName(final String resourceName) {
        if (!resourceName.endsWith(CLASS_SUFFIX)) {
            throw new IllegalArgumentException(
                    String.format("%s is not a valid resource name as it doesn't end with .class", resourceName));
        }

        return resourceName.substring(0, resourceName.length() - CLASS_SUFFIX.length()).replace('/', '.');
    }

    public static boolean isInJdkPackage(String name) {
        return name.startsWith(JAVA) || name.startsWith(JDK_INTERNAL) || name.startsWith(SUN_MISC);
    }
}
