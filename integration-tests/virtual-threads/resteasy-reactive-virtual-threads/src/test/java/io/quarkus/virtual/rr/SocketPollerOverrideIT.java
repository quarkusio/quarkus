package io.quarkus.virtual.rr;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;

/**
 * The native executable only changes the default poller mode: {@code -Djdk.pollerMode=2} still selects the virtual
 * thread pollers. The profile's property is passed to the executable as a system property, so the same executable is
 * started again; nothing is built for this test.
 */
@QuarkusIntegrationTest
@TestProfile(SocketPollerOverrideIT.VirtualThreadPollersProfile.class)
@EnabledOnOs(OS.LINUX)
class SocketPollerOverrideIT {

    @Test
    void explicitVirtualThreadPollerModeIsHonored() {
        String pollers = RestAssured.get("/socket-pollers").then()
                .assertThat().statusCode(200)
                .extract().asString();
        // "native:MasterPoller", or "jvm:MasterPoller" when the integration tests run against the jar
        assertThat(pollers).endsWith(":MasterPoller");
    }

    public static class VirtualThreadPollersProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("jdk.pollerMode", "2");
        }
    }
}
