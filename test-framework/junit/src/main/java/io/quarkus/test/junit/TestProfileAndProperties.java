package io.quarkus.test.junit;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

final class TestProfileAndProperties {
    private final QuarkusTestProfile testProfile;
    private final Map<String, String> properties;

    TestProfileAndProperties(QuarkusTestProfile testProfile, Map<String, String> properties) {
        this.testProfile = testProfile;
        this.properties = properties != null ? properties : Collections.emptyMap();
    }

    public Optional<QuarkusTestProfile> testProfile() {
        return Optional.ofNullable(testProfile);
    }

    Map<String, String> properties() {
        return Collections.unmodifiableMap(properties);
    }

    Optional<String> configProfile() {
        return testProfile().map(QuarkusTestProfile::getConfigProfile);
    }

    boolean isDisabledGlobalTestResources() {
        return testProfile().map(QuarkusTestProfile::disableGlobalTestResources).orElse(false);
    }

    Optional<String> testProfileClassName() {
        return testProfile().map(testProfile -> testProfile.getClass().getName());
    }
}
