package io.quarkus.cyclonedx.endpoint.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CycloneDxEndpointRecorderTest {

    @Test
    void missingHeaderDoesNotAcceptGzip() {
        assertThat(CycloneDxEndpointRecorder.acceptsGzip((String) null)).isFalse();
        assertThat(CycloneDxEndpointRecorder.acceptsGzip("")).isFalse();
    }

    @Test
    void identityDoesNotAcceptGzip() {
        assertThat(CycloneDxEndpointRecorder.acceptsGzip("identity")).isFalse();
        assertThat(CycloneDxEndpointRecorder.acceptsGzip("deflate, br")).isFalse();
    }

    @Test
    void gzipIsAccepted() {
        assertThat(CycloneDxEndpointRecorder.acceptsGzip("gzip")).isTrue();
        assertThat(CycloneDxEndpointRecorder.acceptsGzip("deflate, gzip, br")).isTrue();
        assertThat(CycloneDxEndpointRecorder.acceptsGzip("GZIP")).isTrue();
        assertThat(CycloneDxEndpointRecorder.acceptsGzip("gzip;q=0.5")).isTrue();
    }

    @Test
    void gzipDisabledWithZeroQualityIsNotAccepted() {
        assertThat(CycloneDxEndpointRecorder.acceptsGzip("gzip;q=0")).isFalse();
        assertThat(CycloneDxEndpointRecorder.acceptsGzip("gzip;q=0.0")).isFalse();
        assertThat(CycloneDxEndpointRecorder.acceptsGzip("deflate, gzip;q=0")).isFalse();
    }

    @Test
    void wildcardWithPositiveQualityIsAccepted() {
        assertThat(CycloneDxEndpointRecorder.acceptsGzip("*")).isTrue();
        assertThat(CycloneDxEndpointRecorder.acceptsGzip("*;q=0")).isFalse();
    }

    @Test
    void unparsableQualityFallsBackToAccepted() {
        assertThat(CycloneDxEndpointRecorder.acceptsGzip("gzip;q=abc")).isTrue();
    }
}
