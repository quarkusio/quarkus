package io.quarkus.netty.runtime.graal;

import java.util.function.BooleanSupplier;

/**
 * {@code onlyWith} predicate for the GraalVM substitutions that hard-code the OpenSSL engine as unavailable:
 * true when netty-tcnative is <em>not</em> on the classpath.
 * <p>
 * Keeping those substitutions for the default case means tcnative's JNI surface is not pulled into images that
 * don't ship it. When an application does ship it (for example through {@code io.smallrye:smallrye-openssl}), the
 * original Netty and Vert.x code runs and the Netty extension registers the JNI, resource and run-time
 * initialization metadata the native library needs.
 */
public final class TcnativeAbsent implements BooleanSupplier {

    /** Present in every netty-tcnative flavour (boringssl-static, openssl-dynamic, ...). */
    public static final String PROBE_CLASS = "io.netty.internal.tcnative.SSL";

    @Override
    public boolean getAsBoolean() {
        return isAbsent(TcnativeAbsent.class.getClassLoader());
    }

    static boolean isAbsent(ClassLoader loader) {
        try {
            loader.loadClass(PROBE_CLASS);
            return false;
        } catch (ClassNotFoundException | LinkageError e) {
            return true;
        }
    }
}
