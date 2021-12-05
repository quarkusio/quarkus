package io.quarkus.arc.test.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.test.QuarkusUnitTest;

public class IfBuildPropertyTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Producer.class, AnotherProducer.class,
                            GreetingBean.class, Hello.class, PingBean.class, PongBean.class, FooBean.class, BarBean.class))
            .overrideConfigKey("some.prop1", "v1")
            .overrideConfigKey("some.prop2", "v2");

    @Inject
    Hello hello;

    @Inject
    Instance<BarBean> barBean;

    @Test
    public void testInjection() {
        assertEquals("hello from matching prop. Foo is: foo from missing prop", hello.hello());
        assertEquals("ping", hello.ping());
        assertEquals("pong", hello.pong());
        assertEquals("foo from missing prop", hello.foo());
        assertTrue(barBean.isUnsatisfied());
    }

    @Test
    public void testSelect() {
        assertEquals("hello from matching prop. Foo is: foo from missing prop",
                CDI.current().select(GreetingBean.class).get().greet());
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

    @IfBuildProperty(name = "some.prop1", stringValue = "v") // won't be enabled because the values don't match
    @Singleton
    static class PongBean {

        String pong() {
            return "pong from non matching value";
        }
    }

    static interface FooBean {

        String foo();
    }

    @IfBuildProperty(name = "some.other.prop1", stringValue = "v2")
    static class BarBean {

    }

    @Singleton
    static class Producer {

        @Produces
        @IfBuildProperty(name = "some.prop2", stringValue = "v", enableIfMissing = true) // won't be enabled because the property values don't match - enableIfMissing has no effect when the property does exist
        PingBean nonMatchingPingBean;

        public Producer() {
            this.nonMatchingPingBean = new PingBean() {

                @Override
                String ping() {
                    return "ping dev";
                }

            };
        }

        @Produces
        @IfBuildProperty(name = "some.prop1", stringValue = "v1")
        GreetingBean matchingValueGreetingBean(FooBean fooBean) {
            return new GreetingBean() {

                @Override
                String greet() {
                    return "hello from matching prop. Foo is: " + fooBean.foo();
                }

            };
        }

        @Produces
        @IfBuildProperty(name = "some.prop2", stringValue = "v") // won't be enabled because the property values don't match
        GreetingBean nonMatchingValueGreetingBean(BarBean barBean) {
            return new GreetingBean() {

                @Override
                String greet() {
                    return "hello from dev";
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

    @IfBuildProperty(name = "some.other.prop1", stringValue = "v1", enableIfMissing = true)
    static class AnotherProducer {

        @Produces
        FooBean testFooBean;

        public AnotherProducer() {
            testFooBean = new FooBean() {
                @Override
                public String foo() {
                    return "foo from missing prop";
                }
            };
        }
    }
}
