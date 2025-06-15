package io.quarkus.arc.test.transform.injectionPoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.Unremovable;
import io.quarkus.arc.test.transform.injectionPoint.diffPackage.Foo;
import io.quarkus.arc.test.transform.injectionPoint.diffPackage.SomeBean;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests that a private field can be injected into even though there is no reflection fallback. By default,
 * transformation is used and private fields gets changed to package private. If this option gets turned off, the
 * reflection fallback kicks in.
 */
public class PrivateFieldInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().withApplicationRoot((jar) -> jar
            .addClasses(Head.class, CombineHarvester.class, FooExtended.class, Foo.class, SomeBean.class, Bar.class,
                    Simple.class, SomeInterceptor.class, InterceptedDecoratedBean.class, SomeDecorator.class,
                    DecoratedBean.class, DummyBean.class)
            // this should be on by default but the test states it explicitly to make it clear
            .addAsResource(new StringAsset("quarkus.arc.transform-private-injected-fields=true"),
                    "application.properties"));

    @Test
    public void beanPrivateFieldInjection() {
        assertNotNull(Arc.container().instance(CombineHarvester.class).get().getHead());
        assertNotNull(Arc.container().instance(CombineHarvester.class).get().getBar());
        assertNotNull(Arc.container().instance(FooExtended.class).get().getSomeBean());
    }

    @Test
    public void interceptorDecoratorPrivateFieldInjection() {
        InstanceHandle<InterceptedDecoratedBean> handle = Arc.container().instance(InterceptedDecoratedBean.class);
        assertTrue(handle.isAvailable());
        assertEquals(DummyBean.class.getSimpleName() + InterceptedDecoratedBean.class.getSimpleName()
                + DummyBean.class.getSimpleName(), handle.get().ping());
    }

    @Dependent
    @Simple
    @Unremovable
    static class InterceptedDecoratedBean implements DecoratedBean {
        public String ping() {
            return InterceptedDecoratedBean.class.getSimpleName();
        }
    }

    static interface DecoratedBean {
        public String ping();
    }

    @Dependent
    static class Head {

    }

    @ApplicationScoped
    @Unremovable
    static class CombineHarvester extends BaseHarvester {

        @Inject
        private Head head;

        public Head getHead() {
            return head;
        }

    }

    // Deliberately not a bean but has private injection point
    // this case is not subject to transformation and requires reflection
    static class BaseHarvester {

        @Inject
        private Bar bar;

        public Bar getBar() {
            return bar;
        }
    }

    // this bean extends a bean from another package that has injection into a private field
    @ApplicationScoped
    @Unremovable
    static class FooExtended extends Foo {
    }
}
