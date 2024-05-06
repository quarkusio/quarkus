package io.quarkus.commons.classloading;

public final class ClassloadHelper {

    private ClassloadHelper() {
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

}
