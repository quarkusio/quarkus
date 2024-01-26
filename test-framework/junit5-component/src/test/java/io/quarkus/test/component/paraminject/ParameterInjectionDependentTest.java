package io.quarkus.test.component.paraminject;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import io.quarkus.test.component.QuarkusComponentTest;

@QuarkusComponentTest
public class ParameterInjectionDependentTest {

    @Order(1)
    @Test
    public void testParamsInjection(MyDependent myDependent) {
        assertTrue(myDependent.ping());
    }

    @Order(2)
    @Test
    public void testDependentDestroyed() {
        assertTrue(MyDependent.DESTROYED.get());
    }

    @Dependent
    public static class MyDependent {

        static final AtomicBoolean DESTROYED = new AtomicBoolean();

        boolean ping() {
            return true;
        }

        @PreDestroy
        void destroy() {
            DESTROYED.set(true);
        }
    }

}
