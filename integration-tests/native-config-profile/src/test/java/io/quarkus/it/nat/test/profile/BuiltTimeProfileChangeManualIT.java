package io.quarkus.it.nat.test.profile;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.NativeTestExtension;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

/**
 * Run this manually via {@code mvn clean verify -Dnative -Dit.test=BuiltTimeProfileChangeManualITCase} to ensure that
 * {@link NativeTestExtension#beforeEach(org.junit.jupiter.api.extension.ExtensionContext)} throws an exception caused
 * by an application boot failure. The failure should happen because {@link NativeTestExtension} is setting
 * {@code quarkus.configuration.build-time-mismatch-at-runtime = fail} and
 * {@link BuildProfileChange#getConfigProfile()} returns a profile name that changes
 * {@code quarkus.arc.remove-unused-beans} in {@code application.properties}.
 */
@QuarkusIntegrationTest
@TestProfile(BuiltTimeProfileChangeManualIT.BuildProfileChange.class)
@Disabled("Manual testing only")
public class BuiltTimeProfileChangeManualIT {
    @Test
    public void unusedExists() {
        Assertions.fail("Expected to fail in io.quarkus.test.junit.NativeTestExtension.beforeEach(ExtensionContext)");
    }

    public static class BuildProfileChange implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "build-profile-change";
        }
    }
}
