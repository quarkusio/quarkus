package io.quarkus.arc.test.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.config.ConfigPrefix;
import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.arc.profile.UnlessBuildProfile;
import io.quarkus.test.QuarkusUnitTest;

public class UnlessBuildProfileTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Producer.class, AnotherProducer.class,
                            GreetingBean.class, Hello.class, PingBean.class, PongBean.class, FooBean.class, BarBean.class,
                            TestInterceptor.class, ProdInterceptor.class, Logging.class, DummyTestBean.class,
                            DummyProdBean.class)
                    .addAsResource(
                            new StringAsset(
                                    "%test.dummy.message=Hi from not prod\n" +
                                            "%test.dummy.complex.message=Hi from complex not prod\n" +
                                            "%test.dummy.complex.bis.message=Hi from complex bis not prod\n"),
                            "application.properties"));
    @Inject
    Hello hello;

    @Inject
    Instance<BarBean> barBean;

    @Inject
    DummyTestBean dummyTest;

    @Inject
    Instance<DummyProdBean> dummyProd;

    @Test
    public void testInjection() {
        assertFalse(TestInterceptor.INTERCEPTED.get());
        assertFalse(ProdInterceptor.INTERCEPTED.get());
        assertEquals("hello from not prod. Foo is: foo from not prod", hello.hello());
        assertEquals("ping", hello.ping());
        assertEquals("pong from not prod", hello.pong());
        assertEquals("foo from not prod", hello.foo());
        assertTrue(barBean.isUnsatisfied());
        assertTrue(TestInterceptor.INTERCEPTED.get());
        assertFalse(ProdInterceptor.INTERCEPTED.get());
        assertEquals("Hi from not prod", dummyTest.message);
        assertEquals("Hi from not prod", dummyTest.dummy.message);
        assertEquals("Hi from complex not prod", dummyTest.dummyComplex.message);
        assertEquals("Hi from complex bis not prod", dummyTest.dummyComplexBis.message);
        assertTrue(dummyTest.dummyProd.isUnsatisfied());
        assertTrue(dummyProd.isUnsatisfied());
        assertTrue(hello.dummyAbsent().isUnsatisfied());
        assertTrue(hello.dummyAbsentBis().isUnsatisfied());
    }

    @Test
    public void testSelect() {
        assertEquals("hello from not prod. Foo is: foo from not prod", CDI.current().select(GreetingBean.class).get().greet());
    }

    @ConfigProperties
    public static class Dummy {
        public String message;
    }

    @Singleton
    @UnlessBuildProfile("prod")
    static class DummyTestBean {
        @ConfigProperty(name = "dummy.message")
        String message;
        @Inject
        Dummy dummy;
        @ConfigPrefix("dummy.complex")
        Dummy dummyComplex;
        Dummy dummyComplexBis;
        @Inject
        Instance<DummyProd> dummyProd;

        @Inject
        public void setDummyComplexBis(@ConfigPrefix("dummy.complex.bis") Dummy dummyComplexBis) {
            this.dummyComplexBis = dummyComplexBis;
        }
    }

    @UnlessBuildProfile("test")
    @ConfigProperties(prefix = "dummy.prod")
    public static class DummyProd {
        public String message;
    }

    @Singleton
    @UnlessBuildProfile("test")
    static class DummyProdBean {
        @ConfigProperty(name = "dummy.message")
        String message;
        @ConfigPrefix("dummy.absent") // Should not make it fail as excluded by the UnlessBuildProfile annotation
        Dummy dummyAbsent;
        Dummy dummyAbsentBis;
        @Inject
        DummyProd dummyProd;

        @Inject
        public void setDummyAbsentBis(@ConfigPrefix("dummy.absent.bis") Dummy dummyAbsentBis) { // Should not make it fail as excluded by the UnlessBuildProfile annotation
            this.dummyAbsentBis = dummyAbsentBis;
        }
    }

    @Logging
    @ApplicationScoped
    static class Hello {

        @Inject
        GreetingBean bean;

        @Inject
        PingBean ping;

        @Inject
        PongBean pongBean;

        @Inject
        FooBean fooBean;

        String hello() {
            return bean.greet();
        }

        String ping() {
            return ping.ping();
        }

        String pong() {
            return pongBean.pong();
        }

        String foo() {
            return fooBean.foo();
        }

        Instance<Dummy> dummyAbsent;

        @UnlessBuildProfile("test")
        @ConfigPrefix("dummy.absent.bis")
        Instance<Dummy> dummyAbsentBis;

        @UnlessBuildProfile("test")
        @Inject
        public void setDummyAbsent(@ConfigPrefix("dummy.absent") Instance<Dummy> dummyAbsent) {
            this.dummyAbsent = dummyAbsent;
        }

        Instance<Dummy> dummyAbsent() {
            return dummyAbsent;
        }

        Instance<Dummy> dummyAbsentBis() {
            return dummyAbsentBis;
        }
    }

    @DefaultBean
    @Singleton
    static class GreetingBean {

        String greet() {
            return "hola";
        }
    }

    @DefaultBean
    @Singleton
    static class PingBean {

        String ping() {
            return "ping";
        }
    }

    @UnlessBuildProfile("prod")
    @Singleton
    static class PongBean {

        String pong() {
            return "pong from not prod";
        }
    }

    static interface FooBean {

        String foo();
    }

    @UnlessBuildProfile("test")
    static class BarBean {

    }

    @Singleton
    static class Producer {

        @Produces
        @UnlessBuildProfile("prod")
        GreetingBean notProdGreetingBean(FooBean fooBean) {
            return new GreetingBean() {

                @Override
                String greet() {
                    return "hello from not prod. Foo is: " + fooBean.foo();
                }

            };
        }

        @Produces
        @UnlessBuildProfile("test")
        PingBean notTestPingBean() {
            return new PingBean() {

                @Override
                String ping() {
                    return "ping not test";
                }

            };
        }

        @Produces
        @DefaultBean
        PongBean defaultPongBean() {
            return new PongBean() {
                @Override
                String pong() {
                    return "pong";
                }
            };
        }

    }

    @UnlessBuildProfile("prod")
    static class AnotherProducer {

        @Produces
        FooBean testFooBean() {
            return new FooBean() {
                @Override
                public String foo() {
                    return "foo from not prod";
                }
            };
        }
    }

    @UnlessBuildProfile("prod")
    @Priority(1)
    @Interceptor
    @Logging
    static class TestInterceptor {

        static final AtomicBoolean INTERCEPTED = new AtomicBoolean(false);

        @AroundInvoke
        public Object aroundInvoke(InvocationContext ctx) throws Exception {
            INTERCEPTED.set(true);
            return ctx.proceed();
        }

    }

    @UnlessBuildProfile("test")
    @Priority(10)
    @Interceptor
    @Logging
    static class ProdInterceptor {

        static final AtomicBoolean INTERCEPTED = new AtomicBoolean(false);

        @AroundInvoke
        public Object aroundInvoke(InvocationContext ctx) throws Exception {
            INTERCEPTED.set(true);
            return ctx.proceed();
        }

    }
}
