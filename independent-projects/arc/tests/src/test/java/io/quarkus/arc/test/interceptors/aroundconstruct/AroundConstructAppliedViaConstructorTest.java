package io.quarkus.arc.test.interceptors.aroundconstruct;

import static org.junit.Assert.assertNotNull;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.interceptor.AroundConstruct;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class AroundConstructAppliedViaConstructorTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(MyTransactional.class,
            MyOtherTransactional.class,
            SimpleBean_ConstructorWithInject.class,
            SimpleBean_NoArgsConstructor.class,
            SimpleBean_TwoBindings.class,
            SimpleInterceptor.class,
            OtherInterceptor.class,
            DummyObject.class);

    public static AtomicBoolean INTERCEPTOR_CALLED = new AtomicBoolean(false);
    public static AtomicBoolean OTHER_INTERCEPTOR_CALLED = new AtomicBoolean(false);

    @Before
    public void before() {
        INTERCEPTOR_CALLED.set(false);
        OTHER_INTERCEPTOR_CALLED.set(false);
    }

    @Test
    public void testInterception_constructorWithInject() {
        SimpleBean_ConstructorWithInject simpleBean = Arc.container().instance(SimpleBean_ConstructorWithInject.class).get();
        assertNotNull(simpleBean);
        Assert.assertTrue(INTERCEPTOR_CALLED.get());
    }

    @Test
    public void testInterception_noArgsConstructor() {
        SimpleBean_NoArgsConstructor simpleBean = Arc.container().instance(SimpleBean_NoArgsConstructor.class).get();
        assertNotNull(simpleBean);
        Assert.assertTrue(INTERCEPTOR_CALLED.get());
    }

    @Test
    public void testInterception_twoBindings() {
        SimpleBean_TwoBindings simpleBean = Arc.container().instance(SimpleBean_TwoBindings.class).get();
        assertNotNull(simpleBean);
        Assert.assertTrue(INTERCEPTOR_CALLED.get());
        Assert.assertTrue(OTHER_INTERCEPTOR_CALLED.get());
    }

    @Dependent
    static class DummyObject {

    }

    @Singleton
    static class SimpleBean_ConstructorWithInject {

        @Inject
        @MyTransactional
        public SimpleBean_ConstructorWithInject(DummyObject h) {

        }

    }

    @Singleton
    static class SimpleBean_NoArgsConstructor {

        @MyTransactional
        public SimpleBean_NoArgsConstructor() {

        }

    }

    @Singleton
    @MyOtherTransactional
    static class SimpleBean_TwoBindings {

        @MyTransactional
        public SimpleBean_TwoBindings() {

        }

    }

    @MyTransactional
    @Interceptor
    public static class SimpleInterceptor {

        @AroundConstruct
        void mySuperCoolAroundConstruct(InvocationContext ctx) throws Exception {
            INTERCEPTOR_CALLED.set(true);
            ctx.proceed();
        }

    }

    @MyOtherTransactional
    @Interceptor
    public static class OtherInterceptor {

        @AroundConstruct
        void mySuperCoolAroundConstruct(InvocationContext ctx) throws Exception {
            OTHER_INTERCEPTOR_CALLED.set(true);
            ctx.proceed();
        }

    }
}
