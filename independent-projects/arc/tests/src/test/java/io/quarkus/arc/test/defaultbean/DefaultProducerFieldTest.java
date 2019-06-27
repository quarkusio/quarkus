package io.quarkus.arc.test.defaultbean;

import static org.junit.Assert.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.test.ArcTestContainer;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.junit.Rule;
import org.junit.Test;

public class DefaultProducerFieldTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(Producer.class,
            GreetingBean.class, Hello.class);

    @Test
    public void testInjection() {
        assertEquals("hola", Arc.container().instance(Hello.class).get().hello());
    }

    @Test
    public void testSelect() {
        assertEquals("hola", CDI.current().select(GreetingBean.class).get().greet());
    }

    @ApplicationScoped
    static class Hello {

        @Inject
        GreetingBean bean;

        String hello() {
            return bean.greet();
        }

    }

    @Singleton
    static class GreetingBean {

        String greet() {
            return "hola";
        }
    }

    @Singleton
    static class Producer {

        @DefaultBean
        @Produces
        GreetingBean greetingBean = new GreetingBean() {

            @Override
            String greet() {
                return "hello";
            }

        };

    }

}
