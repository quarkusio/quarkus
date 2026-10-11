package io.quarkus.observability.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.DockerClientFactory;

import io.quarkus.observability.test.support.ConfigEndpoint;
import io.quarkus.observability.test.support.ReloadEndpoint;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

/**
 * Test hot reload, by changing the api path.
 * <p>
 * The reload must reuse the LGTM container started by this process instead of letting the container be rediscovered
 * (and subsequently stopped) by {@code ContainerLocator}; see
 * <a href="https://github.com/quarkusio/quarkus/issues/57067">#57067</a>.
 */
@DisabledOnOs(OS.WINDOWS)
@Tag("devmode")
public class LgtmReloadTest {

    private static final String DEV_SERVICE_LABEL = "io.quarkus.devservice";
    private static final String LGTM_SERVICE_NAME = "lgtm";

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot(
                    jar -> jar.addClasses(ReloadEndpoint.class, ConfigEndpoint.class)
                            .addAsResource("application.properties",
                                    "application.properties"));

    @Test
    public void testReloadKeepsRunningContainer() {
        String containerBeforeReload = awaitRunningLgtmContainer();

        test.modifySourceFile(ReloadEndpoint.class, s -> s.replace("/reload", "/new"));
        awaitReload("/new/poke?f=100");

        String containerAfterReload = findRunningLgtmContainer();
        assertThat(containerAfterReload)
                .as("LGTM dev service container must keep running across a hot reload")
                .isEqualTo(containerBeforeReload);
    }

    private void awaitReload(String path) {
        Awaitility.await()
                .atMost(Duration.ofMinutes(2))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(() -> RestAssured.get(path).then().statusCode(200));
    }

    private String awaitRunningLgtmContainer() {
        return Awaitility.await()
                .atMost(Duration.ofMinutes(5))
                .pollInterval(Duration.ofSeconds(1))
                .until(this::findRunningLgtmContainer, Objects::nonNull);
    }

    private String findRunningLgtmContainer() {
        var containers = DockerClientFactory.instance()
                .client()
                .listContainersCmd()
                .withShowAll(false)
                .withLabelFilter(Map.of(DEV_SERVICE_LABEL, LGTM_SERVICE_NAME))
                .exec();
        return containers.stream().map(c -> c.getId()).findFirst().orElse(null);
    }
}
