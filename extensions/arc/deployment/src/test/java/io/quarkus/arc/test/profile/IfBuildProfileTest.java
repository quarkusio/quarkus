package io.quarkus.arc.test.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.test.QuarkusUnitTest;

public class IfBuildProfileTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Producer.class, OtherProducer.class, AnotherProducer.class,
                            TestInterceptor.class, ProdInterceptor.class, Logging.class));

    @Inject
    Hello hello;

    @Inject
    Instance<BarBean> barBean;

    @Test
    public void testInjection() {
        assertFalse(TestInterceptor.INTERCEPTED.get());
        assertFalse(ProdInterceptor.INTERCEPTED.get());
        assertEquals("hello from test. Foo is: foo from test", hello.hello());
        assertEquals("ping", hello.ping());
        assertEquals("pong", hello.pong());
        assertEquals("foo from test", hello.foo());
        assertTrue(barBean.isUnsatisfied());
        assertTrue(TestInterceptor.INTERCEPTED.get());
        assertFalse(ProdInterceptor.INTERCEPTED.get());
    }

    @Test
    public void testSelect() {
        assertEquals("hello from test. Foo is: foo from test", CDI.current().select(GreetingBean.class).get().greet());
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

    @IfBuildProfile("dev")
    @Singleton
    static class PongBean {

        String pong() {
            return "pong from dev";
        }
    }

    static interface FooBean {

        String foo();
    }

    @IfBuildProfile("dev")
    static class BarBean {

    }

    @Singleton
    static class Producer {

        @Produces
        @IfBuildProfile("test")
        GreetingBean testGreetingBean(FooBean fooBean) {
            return new GreetingBean() {

                @Override
                String greet() {
                    return "hello from test. Foo is: " + fooBean.foo();
                }

            };
        }

        @Produces
        @IfBuildProfile("dev")
        GreetingBean devGreetingBean(BarBean barBean) {
            return new GreetingBean() {

                @Override
                String greet() {
                    return "hello from dev";
                }

            };
        }

        @Produces
        @IfBuildProfile("dev")
        PingBean devPingBean() {
            return new PingBean() {

                @Override
                String ping() {
                    return "ping dev";
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

    @IfBuildProfile("other")
    static class OtherProducer {

        @Produces
        PongBean otherPongBean() {
            return new PongBean() {
                @Override
                String pong() {
                    return "pong from other";
                }
            };
        }
    }

    @IfBuildProfile("test")
    static class AnotherProducer {

        @Produces
        FooBean testFooBean() {
            return new FooBean() {
                @Override
                public String foo() {
                    return "foo from test";
                }
            };
        }
    }

    @IfBuildProfile("test")
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

    @IfBuildProfile("prod")
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
