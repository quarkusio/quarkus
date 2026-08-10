package io.quarkus.devservices.test;

import static io.quarkus.tests.simpleextension.Constants.SIMPLE_EXTENSION_CONTAINER_ID;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

/**
 * Tests that DevServices starts a new container when configuration is incompatible.
 * <p>
 * This test uses Profile2 and checks whether a marker file written by
 * {@link DevServicesContainerIdentityTest} (Profile1) exists in the container.
 * If the marker does not exist, the containers are different, proving that
 * incompatible configuration triggers container replacement.
 */
@QuarkusTest
@TestProfile(DevServicesContainerIdentityIncompatibleTest.Profile2.class)
public class DevServicesContainerIdentityIncompatibleTest {

    @ConfigProperty(name = SIMPLE_EXTENSION_CONTAINER_ID)
    String containerId;

    @Test
    @DisplayName("Verify different container for different profile")
    public void testDifferentProfile() throws Exception {
        assertThat(containerId)
                .as("Container ID should be available")
                .isNotNull();

        // Check if the marker file from Profile1 exists in this container
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
                .as("Container should be different - marker file from Profile1 should not exist in this container")
                .isEqualTo("NOT_FOUND");
    }

    public static class Profile2 implements io.quarkus.test.junit.QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "profile2";
        }
    }
}
