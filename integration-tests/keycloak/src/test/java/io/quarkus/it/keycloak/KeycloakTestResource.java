package io.quarkus.it.keycloak;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class KeycloakTestResource implements QuarkusTestResourceLifecycleManager {
    @Override
    public Map<String, String> start() {
        HashMap<String, String> map = new HashMap<>();

        // a workaround to set system properties defined when executing tests. Looks like this commit introduced an
        // unexpected behavior: 3ca0b323dd1c6d80edb66136eb42be7f9bde3310
        map.put("keycloak.url", System.getProperty("keycloak.url"));

        return map;
    }

    @Override
    public void stop() {

    }
}
