package io.quarkus.arc.test.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.profile.UnlessBuildProfile;
import io.quarkus.test.QuarkusUnitTest;

public class UnlessBuildProfileTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Producer.class, AnotherProducer.class,
                            GreetingBean.class, Hello.class, PingBean.class, PongBean.class, FooBean.class, BarBean.class));

    @Inject
    Hello hello;

    @Inject
    Instance<BarBean> barBean;

    @Test
    public void testInjection() {
        assertEquals("hello from not prod. Foo is: foo from not prod", hello.hello());
        assertEquals("ping", hello.ping());
        assertEquals("pong from not prod", hello.pong());
        assertEquals("foo from not prod", hello.foo());
        assertTrue(barBean.isUnsatisfied());
    }

    @Test
    public void testSelect() {
        assertEquals("hello from not prod. Foo is: foo from not prod", CDI.current().select(GreetingBean.class).get().greet());
    }

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
}
