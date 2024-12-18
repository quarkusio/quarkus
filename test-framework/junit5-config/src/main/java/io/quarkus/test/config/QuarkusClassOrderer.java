package io.quarkus.test.config;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.ClassOrdererContext;

import io.smallrye.config.SmallRyeConfig;

/**
 * A JUnit {@link ClassOrderer}, used to delegate to a custom implementations of {@link ClassOrderer} set by Quarkus
 * config.
 */
public class QuarkusClassOrderer implements ClassOrderer {
    private final ClassOrderer delegate;

    public QuarkusClassOrderer() {
        System.out.println("HOLLY class orderer TCCL is " + Thread.currentThread().getContextClassLoader());
        System.out.println("HOLLY I AM " + this.getClass().getClassLoader());
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        // TODO obviously a bad idea, diagnostic
        delegate = EMPTY;
        //        TestConfig testConfig = config.getConfigMapping(TestConfig.class);
        //
        //        delegate = testConfig.classOrderer()
        //                .map(klass -> ReflectionUtils.tryToLoadClass(klass)
        //                        .andThenTry(ReflectionUtils::newInstance)
        //                        .andThenTry(instance -> (ClassOrderer) instance)
        //                        .toOptional().orElse(EMPTY))
        //                .orElse(EMPTY);
        Thread.currentThread().setContextClassLoader(original);
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
