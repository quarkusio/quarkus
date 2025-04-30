package io.quarkus.it.rest.client.wronghost;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * The only point of this class is to propagate the properties when running the native tests
 */
public class ExternalWrongHostTestResourceUsingVerifyHost implements QuarkusTestResourceLifecycleManager {

    @Override
    public Map<String, String> start() {
        Map<String, String> result = new HashMap<>();
        result.put("wrong-host/mp-rest/trustStore", "target/certs/bad-host-truststore.p12");
        result.put("wrong-host/mp-rest/trustStorePassword", "changeit");
        result.put("wrong-host/mp-rest/verifyHost", Boolean.FALSE.toString());
        return result;
    }

    @Override
    public void stop() {

    }
}
