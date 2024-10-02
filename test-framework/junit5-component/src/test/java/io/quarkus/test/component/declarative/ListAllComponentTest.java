package io.quarkus.test.component.declarative;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.arc.All;
import io.quarkus.test.component.QuarkusComponentTest;

@QuarkusComponentTest
public class ListAllComponentTest {

    @Inject
    @All
    List<SomeBean> components;

    @Test
    public void testComponents() {
        // SomeBean is registered as component
        assertEquals(1, components.size());
    }

}
