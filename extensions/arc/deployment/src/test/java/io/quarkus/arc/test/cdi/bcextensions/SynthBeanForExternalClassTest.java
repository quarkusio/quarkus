package io.quarkus.arc.test.cdi.bcextensions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.Parameters;
import jakarta.enterprise.inject.build.compatible.spi.Synthesis;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticBeanCreator;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticBeanDisposer;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticComponents;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.supplement.SomeClassInExternalLibrary;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusUnitTest;

public class SynthBeanForExternalClassTest {
    // the test includes an _application_ that declares a build compatible extension
    // (in the Runtime CL), which creates a synthetic bean for a class that is _outside_
    // of the application (in the Base Runtime CL)

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyBean.class, MyExtension.class, MySyntheticBeanCreator.class)
                    .addAsServiceProvider(BuildCompatibleExtension.class, MyExtension.class))
            // we need a non-application archive, so cannot use `withAdditionalDependency()`
            .setForcedDependencies(List.of(Dependency.of("io.quarkus", "quarkus-arc-test-supplement", Version.getVersion())));

    @Inject
    MyBean bean;

    @Test
    public void test() {
        assertFalse(MySyntheticBeanCreator.created);
        assertFalse(MySyntheticBeanDisposer.disposed);

        assertEquals("OK", bean.doSomething());
        assertTrue(MySyntheticBeanCreator.created);
        assertTrue(MySyntheticBeanDisposer.disposed);
    }

    @ApplicationScoped
    public static class MyBean {
        @Inject
        Instance<SomeClassInExternalLibrary> someClass;

        public String doSomething() {
            SomeClassInExternalLibrary instance = someClass.get();
            instance.toString(); // force instantiating the bean
            someClass.destroy(instance); // force destroying the instance
            return "OK";
        }
    }

    public static class MyExtension implements BuildCompatibleExtension {
        @Synthesis
        public void synthesis(SyntheticComponents syn) {
            syn.addBean(SomeClassInExternalLibrary.class)
                    .type(SomeClassInExternalLibrary.class)
                    .scope(Dependent.class)
                    .createWith(MySyntheticBeanCreator.class)
                    .disposeWith(MySyntheticBeanDisposer.class);
        }
    }

    public static class MySyntheticBeanCreator implements SyntheticBeanCreator<SomeClassInExternalLibrary> {
        public static boolean created;

        public SomeClassInExternalLibrary create(Instance<Object> lookup, Parameters params) {
            SomeClassInExternalLibrary result = new SomeClassInExternalLibrary();
            created = true;
            return result;
        }
    }

    public static class MySyntheticBeanDisposer implements SyntheticBeanDisposer<SomeClassInExternalLibrary> {
        public static boolean disposed;

        @Override
        public void dispose(SomeClassInExternalLibrary instance, Instance<Object> lookup, Parameters params) {
            disposed = true;
        }
    }
}
