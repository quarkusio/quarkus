package io.quarkus.it.rest.client.trustall;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class ExternalTlsTrustAllTestResource implements QuarkusTestResourceLifecycleManager {

    @Override
    public Map<String, String> start() {
        Map<String, String> result = new HashMap<>();
        result.put("wrong-host/mp-rest/trustStore", System.getProperty("rest-client.trustStore"));
        result.put("wrong-host/mp-rest/trustStorePassword", System.getProperty("rest-client.trustStorePassword"));
        result.put("quarkus.tls.trust-all", "true");
        return result;
    }

    @Override
    public void stop() {

    }
}
