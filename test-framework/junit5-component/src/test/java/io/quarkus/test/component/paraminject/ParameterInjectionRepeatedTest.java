package io.quarkus.test.component.paraminject;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;

import io.quarkus.test.component.QuarkusComponentTest;

@QuarkusComponentTest
public class ParameterInjectionRepeatedTest {
    @RepeatedTest(10)
    public void testParamsInjection(
            // component under test
            Converter converter,
            // ignored automatically, no need for `@SkipInject`
            RepetitionInfo info) {
        assertEquals(10, info.getTotalRepetitions());
        assertEquals("FOOBAR", converter.convert("fooBar"));
    }

    @AfterAll
    public static void afterAll() {
        assertEquals(10, Converter.DESTROYED.get());
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
