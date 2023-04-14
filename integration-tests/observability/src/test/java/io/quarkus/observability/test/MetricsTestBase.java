package io.quarkus.observability.test;

import java.time.Instant;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.List;

import jakarta.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.observability.promql.client.PromQLService;
import io.quarkus.observability.promql.client.data.LabelsResponse;
import io.restassured.RestAssured;

public abstract class MetricsTestBase {

    @Inject
    @RestClient
    PromQLService service;

    final Instant now = Instant.now().with(ChronoField.NANO_OF_SECOND, 0);
    final Instant minus1hour = now.minus(1, ChronoUnit.HOURS);
    final Instant plus1hour = now.plus(1, ChronoUnit.HOURS);

    protected String path() {
        return "/api";
    }

    @Test
    public void testScrapeAndQuery() throws Exception {
        String response = RestAssured.get(path() + "/poke?f=100").body().asString();
        Assertions.assertTrue(response.startsWith("poke:"), response);

        Thread.sleep(30_000); // wait 30sec to scrape ...

        LabelsResponse labelsResponse = service.postLabelValues(
                "__name__",
                "{__name__=~\".*\"}",
                minus1hour,
                plus1hour);
        List<String> data = labelsResponse.data();
        Assertions.assertFalse(data.isEmpty(), "Empty data"); // should be some data
        boolean eXists = data.stream().anyMatch(d -> d.contains("xvalue_X")); // X is on purpose ;-)
        Assertions.assertTrue(eXists, "No test metrics"); // find the gauge
    }

}
