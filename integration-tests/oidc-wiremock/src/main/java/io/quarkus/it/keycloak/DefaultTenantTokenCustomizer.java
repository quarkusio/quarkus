package io.quarkus.it.keycloak;

import jakarta.inject.Singleton;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.TenantFeature;
import io.quarkus.oidc.TokenCustomizer;

@Singleton
@TenantFeature("bearer")
@Unremovable
public class DefaultTenantTokenCustomizer implements TokenCustomizer {

    volatile int rs256Count;

    @Override
    public JsonObject customizeHeaders(JsonObject headers) {
        if (!headers.containsKey("customize")) {
            return null;
        }
        String alg = headers.getString("alg");
        if ("RS256".equals(alg)) {
            if (0 == rs256Count++) {
                return null;
            } else {
                return Json.createObjectBuilder(headers).remove("customize").build();
            }
        } else if ("RS384".equals(alg)) {
            return null;
        } else if ("RS512".equals(alg)) {
            return Json.createObjectBuilder(headers).add("alg", "RS256").build();
        }
        return null;
    }

}
