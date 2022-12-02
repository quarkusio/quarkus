package io.quarkus.it.main;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ProfileManager;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ProfileManagerTestCase {

    private static final String BACKWARD_COMPATIBLE_QUARKUS_PROFILE_PROP = "quarkus-profile";

    @BeforeEach
    public void beforeEach() {
        resetProfileManagerState();
    }

    @AfterEach
    public void afterEach() {
        resetProfileManagerState();
    }

    private void resetProfileManagerState() {
        ProfileManager.setLaunchMode(LaunchMode.TEST); // Tests should be run in LaunchMode.TEST by default
        ProfileManager.setRuntimeDefaultProfile(null);
        System.clearProperty(ProfileManager.QUARKUS_PROFILE_PROP);
        System.clearProperty(ProfileManager.QUARKUS_TEST_PROFILE_PROP);
        System.clearProperty(BACKWARD_COMPATIBLE_QUARKUS_PROFILE_PROP);
        Assertions.assertNull(System.getenv(ProfileManager.QUARKUS_PROFILE_ENV));
    }

    @Test
    public void testDefaultTestProfile() {
        Assertions.assertEquals(LaunchMode.TEST.getDefaultProfile(), ProfileManager.getActiveProfile());
    }

    @Test
    public void testCustomTestProfile() {
        String customProfile = "foo";
        System.setProperty(ProfileManager.QUARKUS_TEST_PROFILE_PROP, customProfile);
        Assertions.assertEquals(customProfile, ProfileManager.getActiveProfile());
    }

    @Test
    public void testCustomNormalProfile() {
        testCustomProfile(LaunchMode.NORMAL);
    }

    @Test
    public void testCustomDevProfile() {
        testCustomProfile(LaunchMode.DEVELOPMENT);
    }

    private void testCustomProfile(LaunchMode launchMode) {
        ProfileManager.setLaunchMode(launchMode);
        ProfileManager.setRuntimeDefaultProfile("foo");
        String customProfile = "bar";
        System.setProperty(ProfileManager.QUARKUS_PROFILE_PROP, customProfile);
        Assertions.assertEquals(customProfile, ProfileManager.getActiveProfile());
    }

    @Test
    public void testBackwardCompatibleCustomNormalProfile() {
        testBackwardCompatibleCustomProfile(LaunchMode.NORMAL);
    }

    @Test
    public void testBackwardCompatibleCustomDevProfile() {
        testBackwardCompatibleCustomProfile(LaunchMode.DEVELOPMENT);
    }

    private void testBackwardCompatibleCustomProfile(LaunchMode launchMode) {
        ProfileManager.setLaunchMode(launchMode);
        ProfileManager.setRuntimeDefaultProfile("foo");
        String customProfile = "bar";
        System.setProperty(BACKWARD_COMPATIBLE_QUARKUS_PROFILE_PROP, customProfile);
        Assertions.assertEquals(customProfile, ProfileManager.getActiveProfile());
    }

    @Test
    public void testCustomRuntimeNormalProfile() {
        testCustomRuntimeProfile(LaunchMode.NORMAL);
    }

    @Test
    public void testCustomRuntimeDevProfile() {
        testCustomRuntimeProfile(LaunchMode.DEVELOPMENT);
    }

    private void testCustomRuntimeProfile(LaunchMode launchMode) {
        ProfileManager.setLaunchMode(launchMode);
        String customProfile = "foo";
        ProfileManager.setRuntimeDefaultProfile(customProfile);
        Assertions.assertEquals(customProfile, ProfileManager.getActiveProfile());
    }

    @Test
    public void testDefaultNormalProfile() {
        testDefaultProfile(LaunchMode.NORMAL);
    }

    @Test
    public void testDefaultDevProfile() {
        testDefaultProfile(LaunchMode.DEVELOPMENT);
    }

    private void testDefaultProfile(LaunchMode launchMode) {
        ProfileManager.setLaunchMode(launchMode);
        Assertions.assertEquals(launchMode.getDefaultProfile(), ProfileManager.getActiveProfile());
    }
}
