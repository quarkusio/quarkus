package io.quarkus.test.component.paraminject;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.SkipInject;

@QuarkusComponentTest
public class ParameterInjectionParameterizedTest {

    @ParameterizedTest
    @ValueSource(strings = { "alpha", "bravo", "delta" })
    public void testParamsInjection(@SkipInject String param, Converter converter) {
        Assertions.assertThat(converter.convert(param)).isIn("ALPHA", "BRAVO", "DELTA");
    }

    @AfterAll
    public static void afterAll() {
        assertEquals(3, Converter.DESTROYED.get());
    }

    @Dependent
    public static class Converter {

        static final AtomicInteger DESTROYED = new AtomicInteger();

        String convert(String param) {
            return param.toUpperCase();
        }

        @PreDestroy
        void destroy() {
            DESTROYED.incrementAndGet();
        }
    }

}
