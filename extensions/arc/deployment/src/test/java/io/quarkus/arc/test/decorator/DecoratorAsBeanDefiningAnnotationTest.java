package io.quarkus.arc.test.decorator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.supplement.decorator.SomeInterface;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;

public class DecoratorAsBeanDefiningAnnotationTest {

    // The test has a CDI bean in the application and a decorator in a second archive that has no other CDI items.
    // The idea is to test that @Decorator is a bean defining annotation and will be picked up.

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClass(SomeBean.class))
            // we need a non-application archive, so cannot use `withAdditionalDependency()`
            .setForcedDependencies(
                    List.of(Dependency.of("io.quarkus", "quarkus-arc-test-supplement-decorator", Version.getVersion())));

    @Inject
    SomeBean bean;

    @Test
    public void test() {
        assertEquals("Delegated: SomeBean", bean.ping());
    }

    @Dependent
    public static class SomeBean implements SomeInterface {

        @Override
        public String ping() {
            return SomeBean.class.getSimpleName();
        }
    }
}
