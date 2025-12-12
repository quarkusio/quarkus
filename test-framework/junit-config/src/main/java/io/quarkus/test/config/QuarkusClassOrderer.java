package io.quarkus.test.config;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.ClassOrdererContext;
import org.junit.platform.commons.util.ReflectionUtils;

import io.quarkus.deployment.dev.testing.TestConfig;
import io.smallrye.config.SmallRyeConfig;

/**
 * A JUnit {@link ClassOrderer}, used to delegate to a custom implementations of {@link ClassOrderer} set by Quarkus
 * config.
 */
public class QuarkusClassOrderer implements ClassOrderer {
    private final ClassOrderer delegate;

    public QuarkusClassOrderer() {

        // Our config will have been initialised onto the classloader that was active when JUnit starts, so read it from there
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

        try {
            SmallRyeConfig config = ConfigProvider.getConfig()
                    .unwrap(SmallRyeConfig.class);
            TestConfig testConfig = config.getConfigMapping(TestConfig.class);

            delegate = testConfig.classOrderer()
                    .map(klass -> ReflectionUtils.tryToLoadClass(klass)
                            .andThenTry(ReflectionUtils::newInstance)
                            .andThenTry(instance -> (ClassOrderer) instance)
                            .toOptional()
                            .orElse(EMPTY))
                    .orElse(EMPTY);
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
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
