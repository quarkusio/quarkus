package io.quarkus.undertow.runtime.graal;

import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.vertx.core.ServiceHelper;
import io.vertx.core.spi.JsonFactory;
import io.vertx.core.spi.json.JsonCodec;

/**
 * Undertow explicitly excludes jackson-core so we need to make sure
 * that Vert.x doesn't blow up when the native image is built
 */

@TargetClass(className = "io.vertx.core.json.Json", onlyWith = JacksonMissingSelector.class)
final class Target_io_vertx_core_json_Json {

    @Substitute
    public static io.vertx.core.spi.JsonFactory load() {
        io.vertx.core.spi.JsonFactory factory = ServiceHelper.loadFactoryOrNull(io.vertx.core.spi.JsonFactory.class);
        if (factory == null) {
            factory = new JsonFactory() {
                @Override
                public JsonCodec codec() {
                    return null;
                }
            };
        }
        return factory;
    }
}

@TargetClass(className = "io.vertx.core.json.jackson.JacksonCodec", onlyWith = JacksonMissingSelector.class)
@Delete
final class Target_io_vertx_core_json_jackson_JacksonCodec {

}

@TargetClass(className = "io.vertx.core.json.jackson.JacksonFactory", onlyWith = JacksonMissingSelector.class)
@Delete
final class Target_io_vertx_core_json_jackson_JacksonFactory {

}

final class JacksonMissingSelector implements BooleanSupplier {

    @Override
    public boolean getAsBoolean() {
        try {
            Class.forName("com.fasterxml.jackson.core.JsonFactory");
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }
}

public class VertxSubstitutions {

}
