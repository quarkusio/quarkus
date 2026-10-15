package io.quarkus.test.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.app.CuratedApplication;

public class DefaultDockerContainerLauncherTest {

    @Test
    void noHintWhenTheNetworkWasReportedByTheBuild() {
        assertThat(DefaultDockerContainerLauncher.devServicesNetworkHint(
                launchResult(Map.of("quarkus.datasource.jdbc.url", "jdbc:postgresql://db:5432/test"), "reported", false)))
                .isNull();
    }

    @Test
    void noHintWithoutDevServices() {
        assertThat(DefaultDockerContainerLauncher.devServicesNetworkHint(
                launchResult(Map.of(), "quarkus-integration-test-abcde", true)))
                .isNull();
    }

    @Test
    void hintWhenTheNetworkWasGeneratedWhileDevServicesConfiguredTheApplication() {
        String hint = DefaultDockerContainerLauncher.devServicesNetworkHint(
                launchResult(Map.of("quarkus.datasource.jdbc.url", "jdbc:postgresql://db:5432/test"),
                        "quarkus-integration-test-abcde", true));

        assertThat(hint)
                .contains("quarkus-integration-test-abcde")
                .contains("quarkus.datasource.jdbc.url")
                .contains("quarkus-devservices-deployment");
    }

    private static ArtifactLauncher.InitContext.DevServicesLaunchResult launchResult(Map<String, String> properties,
            String networkId, boolean manageNetwork) {
        return new ArtifactLauncher.InitContext.DevServicesLaunchResult() {
            @Override
            public Map<String, String> properties() {
                return properties;
            }

            @Override
            public String networkId() {
                return networkId;
            }

            @Override
            public boolean manageNetwork() {
                return manageNetwork;
            }

            @Override
            public CuratedApplication getCuratedApplication() {
                return null;
            }

            @Override
            public void close() {
            }
        };
    }
}
