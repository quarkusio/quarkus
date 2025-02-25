package io.quarkus.it.nat.test.profile;

import java.util.Collections;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.QuarkusIntegrationTestExtension;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

/**
 * Run this manually via {@code mvn clean verify -Dnative -Dit.test=BuiltTimeValueChangeManualITCase} to ensure that
 * {@link QuarkusIntegrationTestExtension#beforeEach(org.junit.jupiter.api.extension.ExtensionContext)} throws an exception
 * caused
 * by an application boot failure. The failure should happen because {@link QuarkusIntegrationTestExtension} is setting
 * {@code quarkus.config.build-time-mismatch-at-runtime = fail} and
 * {@link BuildTimeValueChangeTestProfile#getConfigOverrides()} changes {@code quarkus.arc.remove-unused-beans}.
 */
@QuarkusIntegrationTest
@TestProfile(BuiltTimeValueChangeManualIT.BuildTimeValueChangeTestProfile.class)
@Disabled("Manual testing only")
public class BuiltTimeValueChangeManualIT {
    @Test
    public void failInNativeTestExtension_beforeEach() {
        Assertions.fail("Expected to fail in io.quarkus.test.junit.NativeTestExtension.beforeEach(ExtensionContext)");
    }

    public static class BuildTimeValueChangeTestProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Collections.singletonMap("quarkus.arc.remove-unused-beans", "all");
        }

    }
}
