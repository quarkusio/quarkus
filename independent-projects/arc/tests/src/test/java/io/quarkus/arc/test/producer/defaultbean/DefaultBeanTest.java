package io.quarkus.arc.test.producer.defaultbean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import io.quarkus.arc.Arc;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.test.ArcTestContainer;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Singleton;
import org.junit.Rule;
import org.junit.Test;

public class DefaultBeanTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(Producer1.class, Producer2.class, Producer3.class,
            GreetingBean.class, GreetingService.class);

    @Test
    public void testInjection() {
        final GreetingService greetingBean = Arc.container().instance(GreetingService.class).get();
        assertNotNull(greetingBean);
        assertEquals("ciao!", greetingBean.greet());

    }

    @Test
    public void testSelect() {
        assertEquals("ciao", CDI.current().select(GreetingBean.class).get().greet());
    }

    @Singleton
    static class Producer1 {

        @DefaultBean
        @Produces
        GreetingBean greetingBean() {
            return new GreetingBean("hello");
        }

        @Produces
        GreetingService greetingService(GreetingBean greetingBean) {
            return new GreetingService(greetingBean);
        }

    }

    @Singleton
    static class Producer2 {

        @DefaultBean
        @Produces
        GreetingBean greetingBean() {
            return new GreetingBean("hola");
        }

    }

    @Singleton
    static class Producer3 {

        @Produces
        GreetingBean greetingBean() {
            return new GreetingBean("ciao");
        }
    }

    static class GreetingBean {

        private final String message;

        GreetingBean(String message) {
            this.message = message;
        }

        String greet() {
            return message;
        }
    }

    static class GreetingService {
        private final GreetingBean greetingBean;

        GreetingService(GreetingBean greetingBean) {
            this.greetingBean = greetingBean;
        }

        String greet() {
            return greetingBean.greet() + "!";
        }
    }

}
