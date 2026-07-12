package io.quarkus.test.component.mockthrow;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.InjectMock;
import io.quarkus.test.component.QuarkusComponentTest;

// #55093: thenThrow(Exception.class) must work across component tests (separate class loaders)
@QuarkusComponentTest
public class ThenThrowExceptionClassLoaderTest1 {

    @Inject
    GreetingService service;

    @InjectMock
    GreetingSubService subService;

    @Test
    public void testGreetingService() {
        when(subService.greet("hello there!")).thenThrow(GreetingException.class);
        assertThrows(GreetingException.class, service::greeting);
    }

}
