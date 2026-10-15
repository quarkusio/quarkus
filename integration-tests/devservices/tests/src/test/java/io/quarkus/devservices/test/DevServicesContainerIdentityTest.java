package io.quarkus.devservices.test;

import static io.quarkus.tests.simpleextension.Constants.SIMPLE_EXTENSION_CONTAINER_ID;
import static org.assertj.core.api.Assertions.assertThat;

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
 * Tests that DevServices containers are properly replaced when configuration changes.
 * <p>
 * This test uses Profile1 and writes a marker file inside its container. The companion test
 * {@link DevServicesContainerIdentityIncompatibleTest} uses Profile2 and checks whether the
 * marker file exists. If it does not exist, the containers are different, proving the old
 * container was stopped and replaced when the profile changed.
 * <p>
 * Uses container filesystem state instead of JVM static fields because different test profiles
 * may use different classloaders.
 */
@QuarkusTest
@TestProfile(DevServicesContainerIdentityTest.Profile1.class)
@Order(1)
public class DevServicesContainerIdentityTest {

    static final String MARKER_FILE = "/tmp/profile1-was-here.txt";

    @ConfigProperty(name = SIMPLE_EXTENSION_CONTAINER_ID)
    String containerId;

    @Test
    @DisplayName("Write marker file inside container")
    public void writeMarkerInContainer() throws Exception {
        assertThat(containerId)
                .as("Container ID should be available")
                .isNotNull();

        // Write a marker file inside the container
        DockerClient dockerClient = DockerClientFactory.instance().client();
        ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                .withCmd("sh", "-c", "echo 'profile1' > " + MARKER_FILE)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .exec();

        dockerClient.execStartCmd(execCreateCmdResponse.getId())
                .exec(new com.github.dockerjava.api.async.ResultCallback.Adapter<com.github.dockerjava.api.model.Frame>())
                .awaitCompletion();
    }

    public static class Profile1 implements io.quarkus.test.junit.QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "profile1";
        }
    }

    public static class Profile2 implements io.quarkus.test.junit.QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "profile2";
        }
    }
}
