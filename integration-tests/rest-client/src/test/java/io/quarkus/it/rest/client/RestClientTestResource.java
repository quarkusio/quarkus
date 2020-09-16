package io.quarkus.it.rest.client;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * The only point of this class is to propagate the properties when running the native tests
 */
public class RestClientTestResource implements QuarkusTestResourceLifecycleManager {

    @Override
    public Map<String, String> start() {
        Map<String, String> result = new HashMap<>();
        result.put("rest-client.trustStore", System.getProperty("rest-client.trustStore"));
        result.put("rest-client.trustStorePassword", System.getProperty("rest-client.trustStorePassword"));
        return result;
    }

    @Override
    public void stop() {

    }
}
