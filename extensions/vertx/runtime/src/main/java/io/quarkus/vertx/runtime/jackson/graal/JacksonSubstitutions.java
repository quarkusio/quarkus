package io.quarkus.vertx.runtime.jackson.graal;

import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.vertx.core.json.jackson.JacksonCodec;
import io.vertx.core.spi.json.JsonCodec;

/**
 * When jackson-databind is not on the classpath, substitute {@code QuarkusJacksonFactory.codec()}
 * to avoid referencing classes that require jackson-databind (such as {@code QuarkusJacksonJsonCodec}
 * and {@code DatabindCodec}). GraalVM's static analysis resolves all referenced types at build time,
 * so the try/catch fallback in the original method is not sufficient.
 */
@TargetClass(className = "io.quarkus.vertx.runtime.jackson.QuarkusJacksonFactory", onlyWith = JacksonDatabindMissingSelector.class)
final class Target_QuarkusJacksonFactory {

    @Substitute
    public JsonCodec codec() {
        return new JacksonCodec();
    }
}

/**
 * Same substitution for Vert.x's own {@code JacksonFactory}, which is reachable as the
 * hardcoded fallback in {@code Utils.load()} and also directly references {@code DatabindCodec}
 * in its static initializer.
 */
@TargetClass(className = "io.vertx.core.json.jackson.JacksonFactory", onlyWith = JacksonDatabindMissingSelector.class)
final class Target_JacksonFactory {

    @Substitute
    public JsonCodec codec() {
        return new JacksonCodec();
    }
}

final class JacksonDatabindMissingSelector implements BooleanSupplier {

    @Override
    public boolean getAsBoolean() {
        try {
            Class.forName("com.fasterxml.jackson.databind.ObjectMapper");
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }
}

public class JacksonSubstitutions {
}
