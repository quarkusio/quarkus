package io.quarkus.observability.promql.client.test;

import java.io.InputStream;
import java.time.Duration;
import java.time.Period;
import java.util.List;

import jakarta.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.observability.promql.client.PromQLService;
import io.quarkus.observability.promql.client.data.Dur;
import io.quarkus.observability.promql.client.data.QueryResponse;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class PromQLDataTest {
    private static final Logger log = Logger.getLogger(PromQLDataTest.class);

    @Inject
    @RestClient
    PromQLService service;

    @Inject
    ObjectMapper objectMapper;

    @Test
    public void testInjections() {
        Assertions.assertNotNull(service, "PromQLService not injected");
    }

    @Test
    public void testDeserialize() throws Exception {
        Assertions.assertNotNull(objectMapper, "ObjectMapper not injected");

        for (String rt : List.of("matrix", "scalar", "string", "vector")) {
            try (InputStream stream = getClass().getClassLoader().getResourceAsStream(rt + ".json")) {
                QueryResponse response = objectMapper.readValue(stream, QueryResponse.class);
                Assertions.assertNotNull(response);
                log.infof("response = %s", response);
                Assertions.assertEquals("Dummy", response.errorType());
                Assertions.assertEquals(List.of("W1", "W2"), response.warnings());
            }
        }
    }

    @Test
    public void testDuration() {
        Assertions.assertEquals("1h2m3s", new Dur(Duration.parse("PT1H2M3S")).toString());
        Assertions.assertEquals("2y1w5d", new Dur(Period.parse("P2Y12D")).toString());
        Assertions.assertEquals("2y1w5d1h2m3s", new Dur(Period.parse("P2Y12D"), Duration.parse("PT1H2M3S")).toString());

        testInvalid(null, null);
        testInvalid(Period.ofYears(0), null);
        testInvalid(Period.ofYears(-1), null);
        testInvalid(Period.ofMonths(-1), null);
        testInvalid(Period.ofMonths(1), null);
        testInvalid(Period.ofDays(-1), null);
        testInvalid(null, Duration.ofSeconds(0));
        testInvalid(null, Duration.ofSeconds(-1));
        testInvalid(null, Duration.ofNanos(-1));
    }

    void testInvalid(Period period, Duration duration) {
        try {
            var dur = new Dur(period, duration);
            throw new RuntimeException("Unexpected Dur: " + dur);
        } catch (IllegalArgumentException expected) {
            log.infof("Expected exception: %s", expected.toString());
        }
    }
}
