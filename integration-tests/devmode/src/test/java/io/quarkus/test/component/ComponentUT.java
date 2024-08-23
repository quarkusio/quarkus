package io.quarkus.test.component;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

@QuarkusComponentTest
@TestConfigProperty(key = "bar", value = "qux")
public class ComponentUT {

    @Inject
    ComponentFoo foo;

    @Test
    public void test() {
        assertEquals("qux", foo.ping());
    }

}
