package io.quarkus.it.keycloak;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class NoTestResourceProfile implements QuarkusTestProfile {

    public String getConfigProfile() {
        return "no-test-resource";
    }

    // This profile is supposed to exercise defauly behaviour, but the application.properties changes a lot of defaults, so undo some of those changes
    public Map<String, String> getConfigOverrides() {
        return Map.of("quarkus.keycloak.devservices.create-realm", "true",
                "quarkus.keycloak.policy-enforcer.enabled", "false",
                "quarkus.keycloak.devservices.roles.quarkus", "scratcher,sniffer",
                "quarkus.keycloak.devservices.users.luke", "force1",
                "quarkus.keycloak.devservices.users.leia", "force2");
    }

    @Override
    public boolean disableGlobalTestResources() {
        return true;
    }
}
