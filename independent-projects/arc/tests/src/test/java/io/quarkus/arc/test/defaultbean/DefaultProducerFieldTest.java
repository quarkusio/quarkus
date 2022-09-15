package io.quarkus.arc.test.defaultbean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class DefaultProducerFieldTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Producer.class,
            GreetingBean.class, Hello.class, Fantasy.class);

    @Test
    public void testInjection() {
        assertEquals("hola", Arc.container().instance(Hello.class).get().hello());
    }

    @Test
    public void testSelect() {
        assertEquals("hola", CDI.current().select(GreetingBean.class).get().greet());
    }

    @Test
    public void testInstanceIterator() {
        List<Author> authors = Arc.container().instance(Hello.class).get().instance().stream().collect(Collectors.toList());
        assertEquals(2, authors.size());
        String result = authors.stream().map(Author::get).collect(Collectors.joining());
        assertTrue(result.contains("SciFi"));
        assertTrue(result.contains("Fantasy"));
    }

    @ApplicationScoped
    static class Hello {

        @Inject
        GreetingBean bean;

        @Inject
        Instance<Author> instance;

        String hello() {
            return bean.greet();
        }

        Instance<Author> instance() {
            return instance;
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

        @Produces
        @Singleton
        @DefaultBean
        Author sciFi = new Author() {

            @Override
            public String get() {
                return "SciFi";
            }
        };

    }

    interface Author {
        String get();
    }

    @Singleton
    @DefaultBean
    static class Fantasy implements Author {

        @Override
        public String get() {
            return "Fantasy";
        }
    }

}
