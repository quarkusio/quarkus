package io.quarkus.test.component.declarative;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;

import io.quarkus.test.component.QuarkusComponentTest;

@QuarkusComponentTest
public class BasicDeclarativeTest {
    @Inject
    SimpleComponent component;

    @Test
    public void test() {
        assertEquals("pong", component.ping());
    }

    @Singleton
    static class SimpleComponent {
        String ping() {
            return "pong";
        }
    }
}
