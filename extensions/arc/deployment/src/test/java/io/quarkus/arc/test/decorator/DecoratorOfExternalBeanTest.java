package io.quarkus.arc.test.decorator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.supplement.SomeBeanInExternalLibrary;
import io.quarkus.arc.test.supplement.SomeInterfaceInExternalLibrary;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;

public class DecoratorOfExternalBeanTest {
    // the test includes an _application_ decorator (in the Runtime CL) that applies
    // to a bean that is _outside_ of the application (in the Base Runtime CL)

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClass(MyDecorator.class))
            // we need a non-application archive, so cannot use `withAdditionalDependency()`
            .setForcedDependencies(List.of(Dependency.of("io.quarkus", "quarkus-arc-test-supplement", Version.getVersion())));

    @Inject
    SomeBeanInExternalLibrary bean;

    @Test
    public void test() {
        assertEquals("Delegated: Hello", bean.hello());
    }

    @Decorator
    public static class MyDecorator implements SomeInterfaceInExternalLibrary {
        @Inject
        @Delegate
        SomeInterfaceInExternalLibrary delegate;

        @Override
        public String hello() {
            return "Delegated: " + delegate.hello();
        }
    }
}
