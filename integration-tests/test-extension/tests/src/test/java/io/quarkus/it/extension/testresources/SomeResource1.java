package io.quarkus.it.extension.testresources;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class SomeResource1 implements QuarkusTestResourceLifecycleManager {
    @Override
    public Map<String, String> start() {
        System.err.println(getClass().getSimpleName() + " start");
        return Map.of();
    }

    @Override
    public void stop() {
        System.err.println(getClass().getSimpleName() + " stop");
    }

    @Override
    public void inject(TestInjector testInjector) {
        testInjector.injectIntoFields(getClass().getSimpleName(),
                new TestInjector.AnnotatedAndMatchesType(Resource1Annotation.class, String.class));
    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Resource1Annotation {
    }
}
