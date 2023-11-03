package io.quarkus.it.opentelemetry.jaeger;

import static com.github.dockerjava.api.model.HostConfig.newHostConfig;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;

import jakarta.inject.Inject;

import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.path.json.JsonPath;

@QuarkusTest
public class OTelJaegerRemoteTest {

    private static final int QUERY_PORT = 16686;
    private static final int COLLECTOR_PORT = 14250;
    private static final int HEALTH_PORT = 14269;
    private static final String JAEGER_URL = "http://localhost";
    private static final DockerClient dockerClient;
    @Inject
    OpenTelemetry openTelemetry;

    static {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        DockerHttpClient httpClient = new ZerodepDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .build();
        dockerClient = DockerClientImpl.getInstance(config, httpClient);
        if (dockerClient.listContainersCmd().exec().stream()
                .noneMatch(container -> container.getNames()[0].equals("/jaeger"))) {
            ExposedPort queryPort = ExposedPort.tcp(QUERY_PORT);
            ExposedPort collectorPort = ExposedPort.tcp(COLLECTOR_PORT);
            ExposedPort hostPort = ExposedPort.tcp(HEALTH_PORT);
            Ports portBindings = new Ports();
            portBindings.bind(queryPort, Ports.Binding.bindPort(QUERY_PORT));
            portBindings.bind(collectorPort, Ports.Binding.bindPort(COLLECTOR_PORT));
            portBindings.bind(hostPort, Ports.Binding.bindPort(HEALTH_PORT));
            CreateContainerResponse container = dockerClient
                    .createContainerCmd("ghcr.io/open-telemetry/opentelemetry-java/jaeger:1.32")
                    .withExposedPorts(queryPort, collectorPort, hostPort)
                    .withHostConfig(newHostConfig().withPortBindings(portBindings))
                    .withName("jaeger")
                    .exec();
            dockerClient.startContainerCmd(container.getId()).exec();
        }
    }

    @AfterAll
    static void teardown() {
        dockerClient.listContainersCmd().exec()
                .forEach(container -> {
                    dockerClient.stopContainerCmd(container.getId()).exec();
                    dockerClient.removeContainerCmd(container.getId()).exec();
                });
    }

    @Test
    void testJaegerRemoteIntegration() {
        createTestSpan(openTelemetry);
        JsonPath spanData = Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .until(this::assertJaegerHaveTrace, spanDataObj -> spanDataObj.get("data[0].spans[0].traceID") != null);
        assertNotNull(spanData);
        assertNotNull(spanData.get("data[0].spans[0].spanID"));
        assertEquals("Test span", spanData.get("data[0].spans[0].operationName"));
        assertEquals("Test event", spanData.get("data[0].spans[0].logs[0].fields[0].value"));
        assertEquals("opentelemetry-integration-test-jeager-remote", spanData.get("data[0].processes.p1.serviceName"));
    }

    private void createTestSpan(OpenTelemetry openTelemetry) {
        Span span = openTelemetry.getTracer(getClass().getCanonicalName()).spanBuilder("Test span").startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.addEvent("Test event");
        } catch (Throwable t) {
            span.recordException(t);
            throw t;
        } finally {
            span.end();
        }
    }

    private JsonPath assertJaegerHaveTrace() {
        String serviceName = ConfigProvider.getConfig().getConfigValue("quarkus.application.name").getValue();
        String path = String.format("/api/traces?service=%s", serviceName);

        return given()
                .baseUri(String.format(JAEGER_URL + ":%d", QUERY_PORT))
                .contentType("application/json")
                .when().get(path)
                .body()
                .jsonPath();
    }

}
