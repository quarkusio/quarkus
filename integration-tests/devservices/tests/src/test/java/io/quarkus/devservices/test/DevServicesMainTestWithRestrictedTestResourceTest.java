package io.quarkus.devservices.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainTest;

/**
 * A test resource restricted to the test class must not make every {@link Launch} of a {@link QuarkusMainTest}
 * rebuild the application, otherwise the dev services are restarted between launches of the same test class.
 */
@QuarkusMainTest
@TestProfile(DevServicesMainTestWithRestrictedTestResourceTest.PrintContainerIdProfile.class)
@QuarkusTestResource(value = DevServicesMainTestWithRestrictedTestResourceTest.NoopResource.class, restrictToAnnotatedClass = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DevServicesMainTestWithRestrictedTestResourceTest {

    private static String firstContainerId;

    @Test
    @Order(1)
    @Launch({})
    public void firstLaunch(LaunchResult result) {
        firstContainerId = containerId(result);
        assertThat(firstContainerId).isNotBlank();
    }

    @Test
    @Order(2)
    @Launch({})
    public void secondLaunchReusesTheDevService(LaunchResult result) {
        assertThat(containerId(result))
                .as("the dev service must be reused between launches of the same test class")
                .isEqualTo(firstContainerId);
    }

    private static String containerId(LaunchResult result) {
        return result.getOutputStream().stream()
                .filter(line -> line.startsWith(PrintContainerIdMain.PREFIX))
                .map(line -> line.substring(PrintContainerIdMain.PREFIX.length()).trim())
                .findFirst()
                .orElseThrow(() -> new AssertionError("container id not printed: " + result.getOutput()));
    }

    public static class PrintContainerIdProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.package.main-class", "print-container-id");
        }
    }

    public static class NoopResource implements QuarkusTestResourceLifecycleManager {

        @Override
        public Map<String, String> start() {
            return Map.of();
        }

        @Override
        public void stop() {
        }
    }
}
