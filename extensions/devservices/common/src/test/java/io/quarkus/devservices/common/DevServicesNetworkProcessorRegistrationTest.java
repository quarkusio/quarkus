package io.quarkus.devservices.common;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The build step reporting the Dev Services network id has to ship in the same artifact as
 * {@link ConfigureUtil#configureSharedNetwork}, which is what joins the Dev Services containers to that network.
 * <p>
 * When the two were split across artifacts, an extension depending only on {@code quarkus-devservices-common} joined
 * its containers to the shared network but never reported the network id, so the integration test launcher started the
 * application container on a network of its own making and the application could not resolve the Dev Service hostname.
 * That surfaced 60 seconds later as an unrelated-looking "Unable to determine the status of the running process".
 * <p>
 * Registration depends on the {@code quarkus-extension-processor} annotation processor being configured for this
 * module. Dropping it from the POM would leave the class in place but silently unregistered, so assert on the
 * generated descriptor rather than on the class.
 */
class DevServicesNetworkProcessorRegistrationTest {

    @Test
    void networkIdBuildStepIsRegisteredInThisArtifact() throws IOException {
        assertThat(declaredBuildSteps()).contains(DevServicesNetworkProcessor.class.getName());
    }

    private static List<String> declaredBuildSteps() throws IOException {
        List<String> buildSteps = new ArrayList<>();
        Enumeration<URL> descriptors = DevServicesNetworkProcessorRegistrationTest.class.getClassLoader()
                .getResources("META-INF/quarkus-build-steps.list");
        while (descriptors.hasMoreElements()) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(descriptors.nextElement().openStream(), UTF_8))) {
                reader.lines().map(String::trim).filter(line -> !line.isEmpty()).forEach(buildSteps::add);
            }
        }
        return buildSteps;
    }
}
