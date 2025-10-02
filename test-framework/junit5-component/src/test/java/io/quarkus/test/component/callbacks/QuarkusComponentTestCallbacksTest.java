package io.quarkus.test.component.callbacks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;

import io.quarkus.arc.Arc;
import io.quarkus.test.InjectMock;
import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.TestConfigProperty;
import io.quarkus.test.component.beans.Charlie;
import io.quarkus.test.component.beans.MyComponent;
import io.quarkus.test.component.beans.MyOtherComponent;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@QuarkusComponentTest
public class QuarkusComponentTestCallbacksTest {

    @Inject
    MyComponent myComponent;

    @InjectMock
    Charlie charlie;

    @Order(1)
    @TestConfigProperty(key = "foo", value = "BAR") // overriden by listener
    @Test
    public void testPing() {
        // Note that we cannot test the build functionality of MyTestCallbacks directly (e.g. static fields)
        // because a different CL is used for build
        Mockito.when(charlie.ping()).thenReturn("foo");
        assertEquals("foo and RAB", myComponent.ping());
        // MyOtherComponent added by listener as component class
        // @Unremovable added to MyOtherComponent by listener
        assertEquals("foo", Arc.container().instance(MyOtherComponent.class).get().ping());
        // This should be ok because afterStart is executed on an instance loaded by the QuarkusComponentTestClassLoader
        assertTrue(MyTestCallbacks.afterStart);
        assertFalse(MyTestCallbacks.afterStop);
    }

    @Order(2)
    @Test
    public void testPong() {
        assertTrue(MyTestCallbacks.afterStop);
    }

}
