package io.quarkus.it.opentelemetry.util;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class EndUserProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        Map<String, String> config = new HashMap<>(Map.of("quarkus.otel.traces.eusp.enabled", "true",
                "quarkus.http.auth.permission.roles-1.policy", "role-policy-1",
                "quarkus.http.auth.permission.roles-1.paths", "/otel/enduser/roles-allowed-writer-http-perm-role",
                "quarkus.http.auth.policy.role-policy-1.roles.WRITER", "WRITER-HTTP-PERM"));
        config.put("quarkus.http.auth.policy.role-policy-1.roles.READER", "AUTHZ-FAILURE-ROLE");
        config.put("quarkus.http.auth.policy.role-policy-1.roles-allowed", "WRITER");
        config.put("quarkus.http.auth.permission.roles-2.policy", "role-policy-2");
        config.put("quarkus.http.auth.permission.roles-2.paths", "/otel/enduser/roles-allowed-http-perm-augmentor-role");
        config.put("quarkus.http.auth.policy.role-policy-2.roles-allowed", "AUGMENTOR");
        config.put("quarkus.http.auth.policy.role-policy-2.roles.AUGMENTOR", "HTTP-PERM-AUGMENTOR");
        config.put("quarkus.http.auth.permission.jax-rs.policy", "jax-rs");
        config.put("quarkus.http.auth.permission.jax-rs.paths",
                "/otel/enduser/jax-rs-http-perm,/otel/enduser/jax-rs-http-perm-annotation-reader-role");
        config.put("quarkus.http.auth.permission.jax-rs.applies-to", "JAXRS");
        config.put("quarkus.http.auth.policy.jax-rs.roles-allowed", "WRITER");
        config.put("quarkus.http.auth.permission.permit-all.policy", "permit");
        config.put("quarkus.http.auth.permission.permit-all.paths",
                "/otel/enduser/roles-mapping-http-perm,/otel/enduser/roles-mapping-http-perm-augmentor");
        config.put("quarkus.http.auth.roles-mapping.ROLES-ALLOWED-MAPPING-ROLE", "ROLES-ALLOWED-MAPPING-ROLE-HTTP-PERM");
        return config;
    }
}
