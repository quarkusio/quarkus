package io.quarkus.it.mockbean;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.MockitoConfig;

@QuarkusTest
class GreetingSingletonResourceMockTest {

    // resteasy-reactive adds @Singleton automatically
    @InjectMock
    @MockitoConfig(convertScopes = true)
    GreetingResourceSingleton greetingResourceSingleton;

    @Test
    public void testAutoScope() {
        Mockito.when(greetingResourceSingleton.greet()).thenReturn("Ahoj");
        assertEquals("Ahoj", greetingResourceSingleton.greet());
    }

}
