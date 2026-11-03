package io.quarkus.vertx.http.runtime.graal;

import java.util.OptionalInt;
import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.vertx.core.http.CompressionConfig;

public class BrotliCompressionSupportSubstitutions {
}

@TargetClass(className = "io.quarkus.vertx.http.runtime.options.BrotliCompressionSupport", onlyWith = IsBrotli4jAbsent.class)
final class Target_io_quarkus_vertx_http_runtime_options_BrotliCompressionSupport {

    // The build fails earlier if br is configured without Brotli4j,
    // this only cuts the links to the Netty Brotli classes.
    @Substitute
    static void addBrotli(CompressionConfig compression, OptionalInt compressionLevel) {
        throw new IllegalStateException("Brotli4j is not available");
    }
}

class IsBrotli4jAbsent implements BooleanSupplier {

    // WARNING: There are other copies of this class:
    // - io.quarkus.netty.runtime.graal.IsBrotli4jAbsent
    // - io.quarkus.elasticsearch.restclient.common.runtime.graal.IsBrotli4jAbsent.
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
