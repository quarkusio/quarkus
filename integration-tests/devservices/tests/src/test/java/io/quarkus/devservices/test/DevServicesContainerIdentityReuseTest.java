package io.quarkus.devservices.test;

import static io.quarkus.tests.simpleextension.Constants.SIMPLE_EXTENSION_CONTAINER_ID;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

/**
 * Tests that DevServices reuses containers across test classes with compatible configuration.
 * <p>
 * This test uses the same Profile1 class as {@link DevServicesContainerIdentityTest} and checks
 * whether the marker file written by that test exists in the container. If the marker
 * exists, the containers are the same, proving that compatible configuration triggers
 * container reuse across test classes.
 */
@QuarkusTest
@TestProfile(DevServicesContainerIdentityTest.Profile1.class)
@Order(2)
public class DevServicesContainerIdentityReuseTest {

    @ConfigProperty(name = SIMPLE_EXTENSION_CONTAINER_ID)
    String containerId;

    @Test
    @DisplayName("Verify same container reused across test classes with same profile")
    public void testReuseAcrossTestClasses() throws Exception {
        assertThat(containerId)
                .as("Container ID should be available")
                .isNotNull();

        // Check if the marker file from DevServicesContainerIdentityTest exists in this container
        DockerClient dockerClient = DockerClientFactory.instance().client();
        ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                .withCmd("sh", "-c",
                        "test -f " + DevServicesContainerIdentityTest.MARKER_FILE + " && echo 'EXISTS' || echo 'NOT_FOUND'")
                .withAttachStdout(true)
                .exec();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        dockerClient.execStartCmd(execCreateCmdResponse.getId())
                .exec(new com.github.dockerjava.api.async.ResultCallback.Adapter<com.github.dockerjava.api.model.Frame>() {
                    @Override
                    public void onNext(com.github.dockerjava.api.model.Frame frame) {
                        try {
                            outputStream.write(frame.getPayload());
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                })
                .awaitCompletion();

        String output = outputStream.toString().trim();

        assertThat(output)
                .as("Container should be reused across test classes with same profile - marker file should exist")
                .isEqualTo("EXISTS");
    }
}
