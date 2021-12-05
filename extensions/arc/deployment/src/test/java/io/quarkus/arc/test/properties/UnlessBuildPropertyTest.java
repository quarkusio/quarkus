package io.quarkus.arc.test.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.properties.UnlessBuildProperty;
import io.quarkus.test.QuarkusUnitTest;

public class UnlessBuildPropertyTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Producer.class, AnotherProducer.class,
                            GreetingBean.class, PingBean.class, PongBean.class, FooBean.class, BarBean.class))
            .overrideConfigKey("some.prop1", "v1")
            .overrideConfigKey("some.prop2", "v2");

    @Inject
    GreetingBean bean;

    @Inject
    PingBean ping;

    @Inject
    PongBean pongBean;

    @Inject
    FooBean fooBean;

    @Inject
    Instance<BarBean> barBean;

    @Test
    public void testInjection() {
        assertEquals("hello from matching prop. Foo is: foo from missing prop", bean.greet());
        assertEquals("ping", ping.ping());
        assertEquals("pong", pongBean.pong());
        assertEquals("foo from missing prop", fooBean.foo());
        assertTrue(barBean.isUnsatisfied());
    }

    @Test
    public void testSelect() {
        assertEquals("hello from matching prop. Foo is: foo from missing prop",
                CDI.current().select(GreetingBean.class).get().greet());
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

    @UnlessBuildProperty(name = "some.prop1", stringValue = "v1") // won't be enabled because the value matches
    @UnlessBuildProperty(name = "some.prop1", stringValue = "v")
    @Singleton
    static class PongBean {

        String pong() {
            return "pong from non matching value";
        }
    }

    static interface FooBean {

        String foo();
    }

    @UnlessBuildProperty(name = "some.other.prop1", stringValue = "v2")
    static class BarBean {

    }

    @Singleton
    static class Producer {

        @Produces
        @UnlessBuildProperty(name = "some.prop2", stringValue = "v2", enableIfMissing = true) // won't be enabled because the property values match - enableIfMissing has no effect when the property does exist
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
        @UnlessBuildProperty(name = "some.prop1", stringValue = "v")
        @UnlessBuildProperty(name = "some.prop2", stringValue = "v")
        GreetingBean matchingValueGreetingBean(FooBean fooBean) {
            return new GreetingBean() {

                @Override
                String greet() {
                    return "hello from matching prop. Foo is: " + fooBean.foo();
                }

            };
        }

        @Produces
        @UnlessBuildProperty(name = "some.prop2", stringValue = "v2") // won't be enabled because the property values match
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

    @UnlessBuildProperty(name = "some.other.prop1", stringValue = "v", enableIfMissing = true)
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
