package io.quarkus.elasticsearch.restclient.common.runtime.graal;

import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

// This is an optional decompressing capability of the Apache 5 client.
// It checks for the optional dependency (com.aayushatharva.brotli4j.Brotli4jLoader) at runtime,
// so when the application does not have it, it makes GraalVM happy and cut the links
// to the Brotli4j classes by making it always return false.
@TargetClass(className = "org.apache.hc.client5.http.impl.Brotli4jRuntime", onlyWith = IsBrotli4jAbsent.class)
final class Substitute_Brotli4jRuntime {

    @Substitute
    public static boolean available() {
        return false;
    }
}

class IsBrotli4jAbsent implements BooleanSupplier {

    // WARNING: There are other copies of this class:
    // - io.quarkus.vertx.http.runtime.graal.IsBrotli4jAbsent.
    // - io.quarkus.netty.runtime.graal.IsBrotli4jAbsent.
    // If you change this class, make sure to change the other ones as well.

    @Override
    public boolean getAsBoolean() {
        try {
            // Do not initialize the class: its static initializer loads the native library.
            Class.forName("com.aayushatharva.brotli4j.Brotli4jLoader", false, IsBrotli4jAbsent.class.getClassLoader());
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }
}
