package io.quarkus.test.config;

import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.ClassOrdererContext;
import org.junit.platform.commons.util.ReflectionUtils;

/**
 * A JUnit {@link ClassOrderer}, used to delegate to a custom implementations of {@link ClassOrderer} set by Quarkus
 * config.
 *
 * @deprecated This no longer does anything except delegate to the QuarkusTestProfileAwareClassOrderer. In most cases it will
 *             not be active, unless explicitly configured in.
 */
@Deprecated(forRemoval = true, since = "3.34")
public class QuarkusClassOrderer implements ClassOrderer {
    private final ClassOrderer delegate;

    public QuarkusClassOrderer() {

        String klass = "io.quarkus.test.junit.util.QuarkusTestProfileAwareClassOrderer";
        delegate = ReflectionUtils.tryToLoadClass(klass)
                .andThenTry(ReflectionUtils::newInstance)
                .andThenTry(instance -> (ClassOrderer) instance).toOptional()
                .orElse(EMPTY);

    }

    @Override
    public void orderClasses(final ClassOrdererContext context) {
        delegate.orderClasses(context);
    }

    private static final ClassOrderer EMPTY = new ClassOrderer() {
        @Override
        public void orderClasses(final ClassOrdererContext context) {

        }
    };
}
