package io.quarkus.arc.test.cdi.bcextensions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.Parameters;
import jakarta.enterprise.inject.build.compatible.spi.Synthesis;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticComponents;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticObserver;
import jakarta.enterprise.inject.spi.EventContext;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.supplement.SomeClassInExternalLibrary;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;

public class SynthObserverAsIfInExternalClassTest {
    // the test includes an _application_ that declares a build compatible extension
    // (in the Runtime CL), which creates a synthetic observer which is "as if" declared
    // in a class that is _outside_ of the application (in the Base Runtime CL)

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyBean.class, MyExtension.class, MySyntheticObserver.class)
                    .addAsServiceProvider(BuildCompatibleExtension.class, MyExtension.class))
            // we need a non-application archive, so cannot use `withAdditionalDependency()`
            .setForcedDependencies(List.of(Dependency.of("io.quarkus", "quarkus-arc-test-supplement", Version.getVersion())));

    @Inject
    MyBean bean;

    @Test
    public void test() {
        assertFalse(MySyntheticObserver.notified);

        assertEquals("OK", bean.doSomething());
        assertTrue(MySyntheticObserver.notified);
    }

    @ApplicationScoped
    public static class MyBean {
        @Inject
        Event<String> event;

        public String doSomething() {
            event.fire(""); // force notifying the observer
            return "OK";
        }
    }

    public static class MyExtension implements BuildCompatibleExtension {
        @Synthesis
        public void synthesis(SyntheticComponents syn) {
            syn.addObserver(String.class)
                    .declaringClass(SomeClassInExternalLibrary.class)
                    .observeWith(MySyntheticObserver.class);
        }
    }

    public static class MySyntheticObserver implements SyntheticObserver<String> {
        public static boolean notified;

        @Override
        public void observe(EventContext<String> event, Parameters params) {
            notified = true;
        }
    }
}
