package io.quarkus.vertx.runtime.jackson.graal;

import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.quarkus.vertx.runtime.jackson.QuarkusJacksonFactory;
import io.vertx.core.spi.JsonFactory;

/**
 * Break any links to the Jackson 2 code handling in Vert.x
 */
@TargetClass(JsonFactory.class)
final class Target_JsonFactory {

    @Substitute
    static JsonFactory load() {
        return new QuarkusJacksonFactory();
    }
}

@TargetClass(className = "io.quarkus.vertx.runtime.jackson.QuarkusJacksonFactory$Holder", onlyWith = JacksonDatabindMissingSelector.class)
final class Target_QuarkusJacksonFactoryHolder {

    @Substitute
    private static boolean databindOnClassPath() {
        return false;
    }
}

@TargetClass(className = "io.vertx.core.json.jackson.JacksonFactory")
@Delete
final class Target_JacksonFactory {

}

final class JacksonDatabindMissingSelector implements BooleanSupplier {

    @Override
    public boolean getAsBoolean() {
        try {
            Class.forName("tools.jackson.databind.ObjectMapper");
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }
}

public class JacksonSubstitutions {
}
