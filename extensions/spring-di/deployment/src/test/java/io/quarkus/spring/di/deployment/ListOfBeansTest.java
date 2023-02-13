package io.quarkus.spring.di.deployment;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import io.quarkus.test.QuarkusUnitTest;

public class ListOfBeansTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Foo.class, ServiceAlpha.class, ServiceBravo.class, Service.class,
                            Converter.class, ConverterAlpha.class, ConverterBravo.class));

    @Inject
    Foo foo;

    @Test
    public void testInjection() {
        assertThat(foo.services).hasSize(2).extractingResultOf("ping").containsExactlyInAnyOrder("alpha", "bravo");
        assertThat(foo.converters).hasSize(2).extractingResultOf("pong").containsExactlyInAnyOrder("alpha", "bravo");
    }

    @org.springframework.stereotype.Service
    public static class Foo {

        @Autowired
        List<Service> services;

        final List<Converter> converters;

        @Autowired
        Foo(List<Converter> converters) {
            this.converters = converters;
        }

    }

    public interface Service {

        String ping();
    }

    public interface Converter {

        String pong();
    }

    @Component
    public static class ServiceAlpha implements Service {

        public String ping() {
            return "alpha";
        }
    }

    @Component
    public static class ServiceBravo implements Service {

        public String ping() {
            return "bravo";
        }
    }

    @org.springframework.stereotype.Service
    public static class ConverterAlpha implements Converter {

        @Override
        public String pong() {
            return "alpha";
        }

    }

    @Repository
    public static class ConverterBravo implements Converter {

        @Override
        public String pong() {
            return "bravo";
        }

    }
}
