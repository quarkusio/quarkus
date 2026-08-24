package io.quarkus.grpc.examples.stork;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;

class GrpcStorkMetricsTestBase {

    private static final String SERVICE_NAME = "hello-service";

    @TestHTTPResource
    URI baseUri;

    @Test
    void shouldExposeStorkMetricsAfterBlockingGrpcCall() throws Exception {
        String reply = get("/call/blocking?name=metrics-blocking");
        assertThat(reply).isEqualTo("Hello metrics-blocking");

        Properties snapshot = fetchMetricsSnapshot();
        assertStorkMetricsInMicrometerRegistry(snapshot);
        assertStorkObservation(snapshot);
    }

    @Test
    void shouldExposeStorkMetricsAfterMutinyGrpcCall() throws Exception {
        String reply = get("/call/mutiny?name=metrics-mutiny");
        assertThat(reply).isEqualTo("Hello metrics-mutiny");

        Properties snapshot = fetchMetricsSnapshot();
        assertStorkMetricsInMicrometerRegistry(snapshot);
        assertStorkObservation(snapshot);
    }

    private void assertStorkMetricsInMicrometerRegistry(Properties snapshot) {
        assertThat(Boolean.parseBoolean(snapshot.getProperty("instanceCounter.present"))).isTrue();
        assertThat(Boolean.parseBoolean(snapshot.getProperty("serviceDiscoveryDuration.present"))).isTrue();
        assertThat(Boolean.parseBoolean(snapshot.getProperty("serviceSelectionDuration.present"))).isTrue();
        assertThat(Boolean.parseBoolean(snapshot.getProperty("serviceDiscoveryFailures.present"))).isTrue();
        assertThat(Boolean.parseBoolean(snapshot.getProperty("serviceSelectionFailures.present"))).isTrue();

        assertThat(Double.parseDouble(snapshot.getProperty("serviceSelectionDuration.totalTimeNanos"))).isGreaterThan(0);
        assertThat(Double.parseDouble(snapshot.getProperty("instanceCounter.count"))).isGreaterThan(0);
        assertThat(Double.parseDouble(snapshot.getProperty("serviceDiscoveryDuration.totalTimeNanos"))).isGreaterThan(0);
        assertThat(Double.parseDouble(snapshot.getProperty("serviceDiscoveryFailures.count"))).isZero();
        assertThat(Double.parseDouble(snapshot.getProperty("serviceSelectionFailures.count"))).isZero();
    }

    private void assertStorkObservation(Properties snapshot) {
        assertThat(Boolean.parseBoolean(snapshot.getProperty("observation.present"))).isTrue();
        assertThat(Integer.parseInt(snapshot.getProperty("observation.discoveredInstancesCount"))).isGreaterThan(0);
        assertThat(snapshot.getProperty("observation.serviceName")).isEqualTo(SERVICE_NAME);
        assertThat(Boolean.parseBoolean(snapshot.getProperty("observation.done"))).isTrue();
        assertThat(Boolean.parseBoolean(snapshot.getProperty("observation.failureNull"))).isTrue();
        assertThat(snapshot.getProperty("observation.serviceDiscoveryType")).isEqualTo("static");
        assertThat(snapshot.getProperty("observation.serviceSelectionType")).isEqualTo("round-robin");
        assertThat(Boolean.parseBoolean(snapshot.getProperty("observation.serviceDiscoveryDurationPresent"))).isTrue();
        assertThat(Boolean.parseBoolean(snapshot.getProperty("observation.serviceSelectionDurationPresent"))).isTrue();
    }

    private Properties fetchMetricsSnapshot() throws Exception {
        String body = get("/stork-metrics-snapshot");
        Properties properties = new Properties();
        properties.load(new StringReader(body));
        return properties;
    }

    private String get(String path) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve(path)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        return response.body();
    }
}
