package io.quarkus.it.mockbean;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
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
        assertEquals("Hello from Startup Mocked REST Client", startupService.greetingStartup());
        assertEquals("Hello from Mocked REST Client", startupService.greeting());
    }

    @Produces
    @ApplicationScoped
    @RestClient
    @Alternative
    @Priority(1)
    public GreetingRestClient restClient() {
        GreetingRestClient mock = Mockito.mock(GreetingRestClient.class);
        Mockito.when(mock.greeting()).thenReturn("Hello from Startup Mocked REST Client");
        return mock;
    }
}
