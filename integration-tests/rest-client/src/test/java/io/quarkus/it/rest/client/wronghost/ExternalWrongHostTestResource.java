package io.quarkus.it.rest.client.wronghost;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.restclient.NoopHostnameVerifier;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * The only point of this class is to propagate the properties when running the native tests
 */
public class ExternalWrongHostTestResource implements QuarkusTestResourceLifecycleManager {

    @Override
    public Map<String, String> start() {
        Map<String, String> result = new HashMap<>();
        result.put("wrong-host/mp-rest/trustStore", System.getProperty("rest-client.trustStore"));
        result.put("wrong-host/mp-rest/trustStorePassword", System.getProperty("rest-client.trustStorePassword"));
        result.put("wrong-host/mp-rest/hostnameVerifier", NoopHostnameVerifier.class.getName());
        return result;
    }

    @Override
    public void stop() {

    }
}
