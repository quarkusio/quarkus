package io.quarkus.smallrye.metrics.registration;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Verify that metrics are registered correctly for inherited default methods when a bean is annotated with a
 * metric annotation.
 */
public class DefaultMethodTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(A.class, B.class));

    @Inject
    B b;

    @Inject
    @Named("C")
    C c;

    @Inject
    D d;

    @Inject
    Y y;

    @Inject
    ClashA clashA;

    @Inject
    MetricRegistry metricRegistry;

    @Test
    public void defaultMethodInherited() {
        b.foo();
        b.foo();
        Assertions.assertEquals(2L, metricRegistry.getCounters().get(new MetricID("B.foo")).getCount());
    }

    @Test
    public void defaultMethodOverriddenByMetricBean() {
        c.foo();
        c.foo();
        Assertions.assertEquals(2L, metricRegistry.getCounters().get(new MetricID("C.foo")).getCount());
    }

    @Test
    public void defaultMethodOverriddenBySuperclass() {
        d.foo();
        d.foo();
        Assertions.assertEquals(2L, metricRegistry.getCounters().get(new MetricID("D.foo")).getCount());
    }

    @Test
    public void defaultMethodOverriddenByAnotherInterface() {
        y.foo();
        y.foo();
        Assertions.assertEquals(2L, metricRegistry.getCounters().get(new MetricID("Y.foo")).getCount());
    }

    @Test
    public void twoMethodsWithTheSameNameAreInherited() {
        clashA.doSomething();
        clashA.doSomething();
        Assertions.assertEquals(2L, metricRegistry.getCounters().get(new MetricID("ClashA.doSomething")).getCount());
    }

    interface A {
        default void foo() {

        }
    }

    interface X extends A {
        default void foo() {

        }
    }

    @Dependent
    @Counted(name = "Y", absolute = true)
    static class Y implements X {

    }

    @Dependent
    @Counted(name = "B", absolute = true)
    static class B implements A {

    }

    @Dependent
    @Counted(name = "C", absolute = true)
    @Named("C")
    static class C implements A {
        @Override
        public void foo() {

        }
    }

    @Dependent
    @Counted(name = "D", absolute = true)
    static class D extends C {

    }

    // case with a method name clash - a class inherits two methods with the same name

    @Dependent
    @Counted(name = "ClashA", absolute = true)
    static class ClashA extends ClashB implements I {
    }

    static class ClashB {
        public void doSomething() {
        }
    }

    interface I {
        default void doSomething() {
        }
    }

}
