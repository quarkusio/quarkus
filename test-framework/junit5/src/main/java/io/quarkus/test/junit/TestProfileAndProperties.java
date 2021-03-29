package io.quarkus.test.junit;

import java.util.Map;

final class TestProfileAndProperties {
    final QuarkusTestProfile testProfile;
    final Map<String, String> properties;

    public TestProfileAndProperties(QuarkusTestProfile testProfile, Map<String, String> properties) {
        this.testProfile = testProfile;
        this.properties = properties;
    }
}
