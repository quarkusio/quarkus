package io.quarkus.it.keycloak;

import jakarta.inject.Singleton;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.TenantFeature;
import io.quarkus.oidc.TokenCustomizer;

@Singleton
@TenantFeature({ "bearer", "custombearer" })
@Unremovable
public class BearerTenantsTokenCustomizer implements TokenCustomizer {

    volatile int rs256CountBearer;
    volatile int rs256CountCustomBearer;

    @Override
    public JsonObject customizeHeaders(JsonObject headers) {
        String customizeHeader = null;
        if (headers.containsKey("customize_bearer")) {
            customizeHeader = "customize_bearer";
        } else if (headers.containsKey("customize_custombearer")) {
            customizeHeader = "customize_custombearer";
        }
        if (customizeHeader == null) {
            return null;
        }
        String alg = headers.getString("alg");
        if ("RS256".equals(alg)) {
            if ("customize_bearer".equals(customizeHeader)) {
                if (0 == rs256CountBearer++) {
                    return null;
                } else {
                    return Json.createObjectBuilder(headers).remove(customizeHeader).build();
                }
            } else {
                if (0 == rs256CountCustomBearer++) {
                    return null;
                } else {
                    return Json.createObjectBuilder(headers).remove(customizeHeader).build();
                }
            }
        } else if ("RS384".equals(alg)) {
            return null;
        } else if ("RS512".equals(alg)) {
            return Json.createObjectBuilder(headers).add("alg", "RS256").build();
        }
        return null;
    }

}
