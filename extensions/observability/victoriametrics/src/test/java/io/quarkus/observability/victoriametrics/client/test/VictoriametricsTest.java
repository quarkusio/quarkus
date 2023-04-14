package io.quarkus.observability.victoriametrics.client.test;

import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.observability.devresource.victoriametrics.VictoriaMetricsResource;
import io.quarkus.observability.promql.client.data.Dur;
import io.quarkus.observability.promql.client.data.LabelsResponse;
import io.quarkus.observability.promql.client.data.QueryResponse;
import io.quarkus.observability.promql.client.data.SeriesResponse;
import io.quarkus.observability.promql.client.data.Status;
import io.quarkus.observability.victoriametrics.client.PushGauge;
import io.quarkus.observability.victoriametrics.client.VictoriaMetricsService;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(VictoriaMetricsResource.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisabledOnOs(OS.WINDOWS)
public class VictoriametricsTest {
    private static final Logger log = LoggerFactory.getLogger(VictoriametricsTest.class);

    @Inject
    @RestClient
    VictoriaMetricsService vmService;

    @Inject
    ObjectMapper objectMapper;

    final Instant now = Instant.now().with(ChronoField.NANO_OF_SECOND, 0);
    final Instant minus1hour = now.minus(1, ChronoUnit.HOURS);
    final Instant plus1hour = now.plus(1, ChronoUnit.HOURS);

    @BeforeAll
    public void beforeAll() {
        var height = PushGauge
                .build()
                .namespace("foo")
                .subsystem("bar")
                .name("height")
                .help("foo-bar height in meters")
                .unit("m")
                .labelNames("instance", "module")
                .create();

        var width = PushGauge
                .build()
                .namespace("foo")
                .subsystem("bar")
                .name("width")
                .help("foo-bar width in meters")
                .unit("m")
                .labelNames("instance", "module")
                .create();

        int i = 0;
        for (var t = minus1hour; !t.isAfter(plus1hour); t = t.plus(3, ChronoUnit.MINUTES), i++) {
            var ms = t.toEpochMilli();
            var r = ThreadLocalRandom.current().nextDouble(0, 0.01);

            if ((i % 2) > 0) {
                log.info("Inserting heights for: {} ({} ms)", t, ms);
                height.labels("a", "1").add(0.11 + r, ms);
                height.labels("a", "2").add(0.12 + r, ms);
                height.labels("b", "1").add(0.21 + r, ms);
                height.labels("b", "2").add(0.22 + r, ms);
                height.labels("c", "1").add(0.31 + r, ms);
                height.labels("c", "2").add(0.32 + r, ms);
            }

            if ((i % 3) > 0) {
                log.info("Inserting widths for: {} ({} ms)", t, ms);
                width.labels("a", "1").add(0.11 + r, ms);
                width.labels("a", "2").add(0.12 + r, ms);
                width.labels("b", "1").add(0.21 + r, ms);
                width.labels("b", "2").add(0.22 + r, ms);
                width.labels("c", "1").add(0.31 + r, ms);
                width.labels("c", "2").add(0.32 + r, ms);
            }
        }

        VictoriaMetricsService.importPrometheus(vmService, Stream.of(height, width));
        vmService.flush();
    }

    @Test
    @Order(1)
    @Disabled("Due to resteasy bug not URL-escaping String query parameter(s) having {...} in them")
    public void testGetLabels() {
        LabelsResponse labelsResponse = vmService.getLabels(
                "{__name__=~\".*\"}",
                minus1hour,
                plus1hour);
        log.info("testGetLabels = {}", json(labelsResponse));
    }

    @Test
    @Order(2)
    public void testPostLabels() {
        LabelsResponse labelsResponse = vmService.postLabels(
                "{__name__=~\".*\"}",
                minus1hour,
                plus1hour);
        log.info("testPostLabels = {}", json(labelsResponse));
    }

    @Test
    @Order(3)
    @Disabled("Due to resteasy bug not URL-escaping String query parameter(s) having {...} in them")
    public void testGetLabelValues() {
        LabelsResponse labelsResponse = vmService.getLabelValues(
                "__name__",
                "{__name__=~\".*\"}",
                minus1hour,
                plus1hour);
        log.info("testGetLabelValues = {}", json(labelsResponse));
    }

    @Test
    @Order(4)
    public void testPostLabelValues() {
        LabelsResponse labelsResponse = vmService.postLabelValues(
                "__name__",
                "{__name__=~\".*\"}",
                minus1hour,
                plus1hour);
        log.info("testPostLabelValues = {}", json(labelsResponse));
    }

    @Test
    @Order(5)
    @Disabled("Due to resteasy bug not URL-escaping String query parameter(s) having {...} in them")
    public void testGetSeries() {
        SeriesResponse seriesResponse = vmService.getSeries(
                "{__name__=~\"foo_bar_.*\"}",
                minus1hour,
                plus1hour);
        log.info("testGetSeries = {}", json(seriesResponse));
    }

    @Test
    @Order(6)
    public void testPostSeries() {
        SeriesResponse seriesResponse = vmService.postSeries(
                "{__name__=~\"foo_bar_.*\"}",
                minus1hour,
                plus1hour);
        log.info("testPostSeries = {}", json(seriesResponse));
    }

    @Test
    @Order(7)
    public void testGetInstantQuery() {
        QueryResponse queryResponse = vmService.getInstantQuery(
                "foo_bar_height_m{} + foo_bar_width_m{}",
                now,
                new Dur(Duration.ofMinutes(1)));
        log.info("testGetInstantQuery = {}", json(queryResponse));
    }

    @Test
    @Order(8)
    public void testPostInstantQuery() {
        QueryResponse queryResponse = vmService.postInstantQuery(
                "foo_bar_height_m{} + foo_bar_width_m{}",
                now,
                new Dur(Duration.ofMinutes(1)));
        log.info("testPostInstantQuery = {}", json(queryResponse));
    }

    @Test
    @Order(9)
    public void testGetRangeQuery() {
        QueryResponse queryResponse = vmService.getRangeQuery(
                "foo_bar_height_m{} + foo_bar_width_m{}",
                minus1hour,
                plus1hour,
                new Dur(Duration.ofMinutes(5)),
                new Dur(Duration.ofMinutes(1)));
        log.info("testGetRangeQuery = {}", json(queryResponse));
    }

    @Test
    @Order(10)
    public void testPostRangeQuery1() {
        QueryResponse queryResponse = vmService.postRangeQuery(
                "foo_bar_height_m{} + foo_bar_width_m{}",
                minus1hour,
                plus1hour,
                new Dur(Duration.ofMinutes(5)),
                new Dur(Duration.ofMinutes(1)));
        log.info("testPostRangeQuery1 = {}", json(queryResponse));
    }

    @Test
    @Order(10)
    public void testPostRangeQuery2() {
        QueryResponse queryResponse = vmService.postRangeQuery(
                "{__name__=~\"foo_bar_.*\"}",
                minus1hour,
                plus1hour,
                new Dur(Duration.ofMinutes(15)),
                new Dur(Duration.ofMinutes(1)));
        log.info("testPostRangeQuery2 = {}", json(queryResponse));
    }

    @Test
    @Order(11)
    public void testDeleteSeries() throws Exception {
        var selector = "{__name__=~\"foo_bar_.*\"}";
        log.info("postSeries({})...", selector);
        var series = vmService.postSeries(selector, minus1hour, plus1hour);
        Assertions.assertEquals(Status.success, series.status());
        series.data().forEach(m -> log.info("{}", json(m)));
        log.info("Deleting it...");
        vmService.deleteSeries(selector);
        series = vmService.postSeries(selector, minus1hour, plus1hour);
        log.info("postSeries({})...", selector);
        series.data().forEach(m -> log.info("{}", json(m)));
        log.info("...done.");
        Assertions.assertEquals(List.of(), series.data());
    }

    private String json(Object o) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }
}
