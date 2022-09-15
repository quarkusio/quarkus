package io.quarkus.security.webauthn;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.FormParam;

import io.vertx.core.json.JsonObject;

/**
 * JAX-RS structure suitable for use as a {@link BeanParam} for a POST response
 * containing all the required fields for a form-based registration.
 *
 * @see WebAuthnController#callback(io.vertx.ext.web.RoutingContext) for a JSON-based registration
 */
public class WebAuthnRegisterResponse extends WebAuthnResponse {
    /**
     * Corresponds to the JSON {@code response.attestationObject} field
     */
    @FormParam("webAuthnResponseAttestationObject")
    public String webAuthnResponseAttestationObject;

    @Override
    protected void toJsonObject(JsonObject response) {
        if (webAuthnResponseAttestationObject != null)
            response.put("attestationObject", webAuthnResponseAttestationObject);
    }
}
