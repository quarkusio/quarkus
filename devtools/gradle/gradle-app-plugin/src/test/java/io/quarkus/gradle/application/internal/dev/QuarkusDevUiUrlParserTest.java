package io.quarkus.gradle.application.internal.dev;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class QuarkusDevUiUrlParserTest {

    @Test
    void extractsTheFirstMainListenerAndIgnoresManagement() {
        assertThat(QuarkusDevUiUrlParser.parse(
                "2026-07-27 12:00:00 INFO  [io.quarkus] Listening on: http://localhost:8181 and "
                        + "https://localhost:8443. Management interface listening on http://localhost:9000."))
                .contains("http://localhost:8181/q/dev-ui/continuous-testing");
    }

    @Test
    void supportsHttpsOnlyAndAnsiDecoratedOutput() {
        assertThat(QuarkusDevUiUrlParser.parse(
                "\u001B[32mListening on: https://dev.example.test:9443\u001B[0m"))
                .contains("https://dev.example.test:9443/q/dev-ui/continuous-testing");
    }

    @Test
    void makesWildcardListenersBrowserFriendly() {
        assertThat(QuarkusDevUiUrlParser.parse("Listening on: http://0.0.0.0:49152"))
                .contains("http://localhost:49152/q/dev-ui/continuous-testing");
        assertThat(QuarkusDevUiUrlParser.parse("Listening on: http://[::]:49153"))
                .contains("http://localhost:49153/q/dev-ui/continuous-testing");
    }

    @Test
    void doesNotTreatAManagementOnlyListenerAsTheApplicationListener() {
        assertThat(QuarkusDevUiUrlParser.parse(
                "Listening on: unix:/tmp/quarkus.sock. Management interface listening on http://localhost:9000."))
                .isEmpty();
    }

    @Test
    void ignoresUnrelatedAndMalformedLines() {
        assertThat(QuarkusDevUiUrlParser.parse("Listening for transport dt_socket at address: 5005")).isEmpty();
        assertThat(QuarkusDevUiUrlParser.parse("Listening on: http://localhost")).isEmpty();
        assertThat(QuarkusDevUiUrlParser.parse("Listening on: http://localhost:99999")).isEmpty();
        assertThat(QuarkusDevUiUrlParser.parse("Application started")).isEmpty();
    }
}
