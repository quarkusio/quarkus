package io.quarkus.it.mockbean;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class StartupTest {
    @InjectMock
    @RestClient
    GreetingRestClient greetingRestClient;
    @Inject
    StartupService startupService;

    @Test
    void startup() {
        Mockito.when(greetingRestClient.greeting()).thenReturn("Hello from Mocked REST Client");
        assertEquals("Hello from Mocked REST Client", startupService.greeting());
    }
}
