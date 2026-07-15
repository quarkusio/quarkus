package io.quarkus.test.component.mockthrow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.InjectMock;
import io.quarkus.test.component.QuarkusComponentTest;

// Companion to ThenThrowExceptionClassLoaderTest1 (#55093)
@QuarkusComponentTest
public class ThenThrowExceptionClassLoaderTest2 {

    @Inject
    OtherGreetingService service;

    @InjectMock
    GreetingSubService subService;

    @Test
    public void testGreetingService() {
        when(subService.greet("hello there!")).thenThrow(GreetingException.class);
        try {
            subService.greet("hello there!");
        } catch (Exception e) {
            // The thrown exception must come from this test's class loader
            assertEquals(GreetingException.class.getClassLoader(), e.getClass().getClassLoader());
            assertEquals(GreetingException.class, e.getClass());
        }
        assertThrows(GreetingException.class, service::greeting);
    }

}
