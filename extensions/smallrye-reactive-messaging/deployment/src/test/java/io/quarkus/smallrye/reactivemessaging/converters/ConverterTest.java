package io.quarkus.smallrye.reactivemessaging.converters;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.MessageConverter;

public class ConverterTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Person.class, MyAppUsingConverter.class, MyPersonConverter.class));

    @Inject
    MyAppUsingConverter app;

    @Test
    public void testConverter() {
        assertThat(app.list()).hasSize(4);
        assertThat(app.list().stream().map(new Function<Person, String>() {
            @Override
            public String apply(Person p) {
                return p.name;
            }
        }).collect(Collectors.toList()))
                .containsExactly("john", "paul", "ringo", "george");
    }

    public static class Person {
        public final String name;

        public Person(String name) {
            this.name = name;
        }
    }

    @ApplicationScoped
    public static class MyAppUsingConverter {
        List<Person> list = new ArrayList<>();

        @Outgoing("people")
        public Multi<String> getPeople() {
            return Multi.createFrom().items("john", "paul", "ringo", "george");
        }

        @Incoming("people")
        public void consume(Person p) {
            list.add(p);
        }

        public List<Person> list() {
            return list;
        }
    }

    @ApplicationScoped
    public static class MyPersonConverter implements MessageConverter {

        @Override
        public boolean canConvert(Message<?> in, Type target) {
            return target.equals(Person.class) && in.getPayload().getClass().equals(String.class);
        }

        @Override
        public Message<?> convert(Message<?> in, Type target) {
            return in.withPayload(new Person((String) in.getPayload()));
        }
    }
}
