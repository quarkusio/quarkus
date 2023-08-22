package io.quarkus.test.component;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class BasicTest {
    @RegisterExtension
    static final QuarkusComponentTestExtension extension = new QuarkusComponentTestExtension(SimpleComponent.class);

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
