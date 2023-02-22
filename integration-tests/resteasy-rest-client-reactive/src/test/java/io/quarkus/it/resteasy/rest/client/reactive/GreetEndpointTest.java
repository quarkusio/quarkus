package io.quarkus.it.resteasy.rest.client.reactive;

import io.quarkus.hibernate.validator.runtime.jaxrs.ResteasyReactiveViolationException;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@QuarkusTest
@TestProfile(GreetEndpointTest.FrenchGreeting.class)
class GreetEndpointTest {

    private final GreetClient client;

    GreetEndpointTest(@RestClient GreetClient client) {
        this.client = client;
    }

    @Test
    void greet() {
        var g = client.greet("John");

        assertThat(g)
                .isNotNull()
                .extracting(Greet::getGreeting, Greet::getWho, Greet::getNumber)
                .containsExactly("Bonjour", "John", 1);
    }

    @Test
    void validationFailure() {
        assertThatCode(() -> client.greet("johnny"))
                .isInstanceOf(ResteasyReactiveViolationException.class)
                .hasMessageContaining("greet.name: must match \"^[A-Z][a-z]+$\"");
    }

    public static class FrenchGreeting implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("greeting", "Bonjour");
        }
    }
}
