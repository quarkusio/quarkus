package io.quarkus.test.config;

import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.ClassOrdererContext;
import org.junit.platform.commons.util.ReflectionUtils;

import io.quarkus.deployment.dev.testing.TestConfig;
import io.smallrye.config.Config;

/**
 * A JUnit {@link ClassOrderer}, used to delegate to a custom implementations of {@link ClassOrderer} set by Quarkus
 * config.
 */
public class QuarkusClassOrderer implements ClassOrderer {
    private final ClassOrderer delegate;

    public QuarkusClassOrderer() {
        TestConfig testConfig = Config.get(ClassLoader.getSystemClassLoader()).getConfigMapping(TestConfig.class);
        delegate = testConfig.classOrderer()
                .map(klass -> ReflectionUtils.tryToLoadClass(klass)
                        .andThenTry(ReflectionUtils::newInstance)
                        .andThenTry(instance -> (ClassOrderer) instance)
                        .toOptional()
                        .orElse(EMPTY))
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
