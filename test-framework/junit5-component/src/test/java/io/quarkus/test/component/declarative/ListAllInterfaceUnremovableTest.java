package io.quarkus.test.component.declarative;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.arc.All;
import io.quarkus.test.component.QuarkusComponentTest;

@QuarkusComponentTest
public class ListAllInterfaceUnremovableTest {

    @Inject
    @All
    List<SomeInterface> components;

    @Test
    public void testComponents() {
        assertEquals(1, components.size());
    }

    @Dependent
    public static class Foo implements SomeInterface {
    }

}
