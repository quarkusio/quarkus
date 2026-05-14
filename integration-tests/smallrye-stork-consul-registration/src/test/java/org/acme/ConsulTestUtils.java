package org.acme;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import io.restassured.RestAssured;

public final class ConsulTestUtils {

    private static final String CONSUL_SERVICES_URL = "http://localhost:8500/v1/agent/services";
    private static final String CONSUL_SERVICE_URL = "http://localhost:8500/v1/agent/service/";

    private ConsulTestUtils() {
    }

    static String findServiceId(String serviceName) {
        Map<String, ?> services = RestAssured.get(CONSUL_SERVICES_URL)
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getMap(".");
        return services.keySet().stream()
                .filter(id -> id.startsWith(serviceName))
                .findFirst()
                .orElse(null);
    }

    static String serviceUrl(String serviceName) {
        String serviceId = findServiceId(serviceName);
        assertNotNull(serviceId, "Service '" + serviceName + "' should be registered in Consul");
        return CONSUL_SERVICE_URL + serviceId;
    }

    static void assertServiceNotRegistered(String serviceName) {
        assertNull(findServiceId(serviceName),
                "Service '" + serviceName + "' should not be registered in Consul");
    }
}