package io.quarkus.it.resteasy.rest.client.classic;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@QuarkusTest
@TestProfile(GreetEndpointTest.PolishGreeting.class)
class GreetEndpointTest {

    private final GreetClient client;

    GreetEndpointTest(@RestClient GreetClient client) {
        this.client = client;
    }

    @Test
    void greet() {
        var g = client.greet("Mark");

        assertThat(g)
                .isNotNull()
                .extracting(Greet::getGreeting, Greet::getWho, Greet::getNumber)
                .containsExactly("Witaj", "Mark", 1);
    }

    @Test
    void validationFailure() {
        assertThatCode(() -> client.greet("marky"))
                .isInstanceOf(WebApplicationException.class)
                .hasMessageContaining("Unknown error, status code 400");
    }

    public static class PolishGreeting implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("greeting", "Witaj");
        }
    }
}
