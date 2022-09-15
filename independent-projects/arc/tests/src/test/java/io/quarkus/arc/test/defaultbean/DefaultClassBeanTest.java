package io.quarkus.arc.test.defaultbean;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class DefaultClassBeanTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Producer.class,
            GreetingBean.class, Hello.class, PingBean.class, Author.class, SciFi.class, Fantasy.class, Detective.class);

    @Test
    public void testInjection() {
        Hello hello = Arc.container().instance(Hello.class).get();
        assertEquals("hello", hello.hello());
        assertEquals("pong", hello.ping());
        var result = hello.instance();
        StringBuilder sb = new StringBuilder();
        for (var i : result) {
            i.write(sb);
        }
        Assertions.assertTrue(sb.toString().contains("SciFi"));
        Assertions.assertTrue(sb.toString().contains("Fantasy"));
        Assertions.assertFalse(sb.toString().contains("Detective"));
        Set<Detective> detectives = Arc.container().beanManager().createInstance().select(Detective.class).stream()
                .collect(Collectors.toSet());
        Assertions.assertEquals(1, detectives.size());
    }

    @Test
    public void testSelect() {
        assertEquals("hello", CDI.current().select(GreetingBean.class).get().greet());
    }

    @ApplicationScoped
    static class Hello {

        @Inject
        GreetingBean bean;

        @Inject
        PingBean ping;

        @Inject
        Instance<Author> instance;

        String hello() {
            return bean.greet();
        }

        String ping() {
            return ping.ping();
        }

        Instance<Author> instance() {
            return instance;
        }
    }

    @DefaultBean // This one is overriden by Producer.greetingBean()
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
            return "pong";
        }
    }

    @Singleton
    static class Producer {

        @Produces
        GreetingBean greetingBean() {
            return new GreetingBean() {

                @Override
                String greet() {
                    return "hello";
                }

            };
        }

    }

    interface Author {
        void write(StringBuilder sb);
    }

    @Singleton
    static class SciFi implements Author {

        @Override
        public void write(StringBuilder sb) {
            sb.append("SciFi");
        }
    }

    @Singleton
    static class Fantasy implements Author {

        @Override
        public void write(StringBuilder sb) {
            sb.append("Fantasy");
        }
    }

    @Singleton
    @DefaultBean
    static class Detective implements Author {

        @Override
        public void write(StringBuilder sb) {
            sb.append("Detective");
        }
    }
}
