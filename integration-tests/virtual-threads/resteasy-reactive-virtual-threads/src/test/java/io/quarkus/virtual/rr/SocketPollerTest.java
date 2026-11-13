package io.quarkus.virtual.rr;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
class SocketPollerTest {

    /**
     * A native executable must not use the virtual thread pollers, in which a sub-poller can lose its wakeup
     * (https://github.com/oracle/graal/issues/15051). The JVM keeps the JDK's default.
     */
    @Test
    void nativeExecutableUsesPlatformThreadPollers() {
        String pollers = RestAssured.get("/socket-pollers").then()
                .assertThat().statusCode(200)
                .extract().asString();
        if (pollers.startsWith("native:")) {
            //If the VirtualThreadPollerSubstitutions wasn't effective, we would see "MasterPoller" here,
            //assuming it's run on Linux. Other platforms default to the same effect as what our substitution does.
            assertThat(pollers).isEqualTo("native:Read-Poller,Write-Poller");
        } else {
            assertThat(pollers).startsWith("jvm:").contains("Poller");
        }
    }
}
