package io.quarkus.test.component.declarative;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.arc.All;
import io.quarkus.test.component.QuarkusComponentTest;

@QuarkusComponentTest
public class ListAllInterfaceTest {

    @Inject
    @All
    List<SomeInterface> components;

    @Test
    public void testComponents() {
        // SomeInterface is registered as component but no implementation is added automatically
        assertEquals(0, components.size());
    }

}
