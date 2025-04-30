package io.quarkus.test.component.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

import io.quarkus.test.InjectMock;
import io.quarkus.test.component.QuarkusComponentTestExtension;
import io.quarkus.test.component.beans.Charlie;

@TestInstance(Lifecycle.PER_CLASS)
public class PerClassLifecycleTest {

    @RegisterExtension
    static final QuarkusComponentTestExtension extension = new QuarkusComponentTestExtension();

    @Inject
    MySingleton mySingleton;

    @InjectMock
    Charlie charlie;

    @Order(1)
    @Test
    public void testPing1() {
        Mockito.when(charlie.ping()).thenReturn("foo");
        assertEquals("foo", mySingleton.ping());
        assertEquals(1, MySingleton.COUNTER.get());
    }

    @Order(2)
    @Test
    public void testPing2() {
        Mockito.when(charlie.ping()).thenReturn("baz");
        assertEquals("baz", mySingleton.ping());
        assertEquals(1, MySingleton.COUNTER.get());
    }

    @Singleton
    public static class MySingleton {

        static final AtomicInteger COUNTER = new AtomicInteger();

        @Inject
        Charlie charlie;

        @PostConstruct
        void init() {
            COUNTER.incrementAndGet();
        }

        public String ping() {
            return charlie.ping();
        }

    }

}
