package io.quarkus.it.testsupport.commandmode;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainTest;

@QuarkusMainTest
@TestProfile(QuarkusMainTestWithTestProfileAndFailingApplicationTestCase.MyTestProfile.class)
public class QuarkusMainTestWithTestProfileAndFailingApplicationTestCase {

    @Test
    @Launch(value = {}, exitCode = 1)
    public void testLaunchCommand(LaunchResult result) {
        assertThat(result.getOutput()).contains("dummy");
    }

    public static class MyTestProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.package.main-class", "failing-application");
        }
    }
}
