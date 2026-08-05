package io.quarkus.deployment.builditem.nativeimage;

/**
 * Required by GraalVM's FFM API reachability metadata.
 * Maps Panama ValueLayouts to the strings expected by native-image.
 *
 * https://github.com/openjdk/jdk25u/blob/master/src/java.base/share/classes/java/lang/foreign/ValueLayout.java
 */
public enum FfmType {

    // Used for function return types.
    VOID("void"),
    // ValueLayout.ADDRESS, or any C pointer (void*)
    ADDRESS("void*"),
    // ValueLayout.JAVA_INT (32-bit integer)
    INT("jint"),
    // ValueLayout.JAVA_LONG (64-bit integer)
    LONG("jlong"),
    // ValueLayout.JAVA_FLOAT (32-bit floating point)
    FLOAT("jfloat"),
    // ValueLayout.JAVA_DOUBLE (64-bit floating point)
    DOUBLE("jdouble"),
    // ValueLayout.JAVA_BOOLEAN
    BOOLEAN("jboolean"),
    // ValueLayout.JAVA_BYTE
    BYTE("jbyte"),
    // ValueLayout.JAVA_CHAR
    CHAR("jchar"),
    // ValueLayout.JAVA_SHORT
    SHORT("jshort");

    private final String canonicalName;

    FfmType(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public String getCanonicalName() {
        return canonicalName;
    }
}
