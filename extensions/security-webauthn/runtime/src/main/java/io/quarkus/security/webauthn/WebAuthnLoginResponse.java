package io.quarkus.security.webauthn;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.FormParam;

import io.vertx.core.json.JsonObject;

/**
 * JAX-RS structure suitable for use as a {@link BeanParam} for a POST response
 * containing all the required fields for a form-based login.
 *
 * @see WebAuthnController#callback(io.vertx.ext.web.RoutingContext) for a JSON-based login
 */
public class WebAuthnLoginResponse extends WebAuthnResponse {
    /**
     * Corresponds to the JSON {@code response.authenticatorData} field
     */
    @FormParam("webAuthnResponseAuthenticatorData")
    public String webAuthnResponseAuthenticatorData;

    /**
     * Corresponds to the JSON {@code response.signature} field
     */
    @FormParam("webAuthnResponseSignature")
    public String webAuthnResponseSignature;

    /**
     * Corresponds to the JSON {@code response.userHandle} field
     */
    @FormParam("webAuthnResponseUserHandle")
    public String webAuthnResponseUserHandle;

    @Override
    protected void toJsonObject(JsonObject response) {
        if (webAuthnResponseAuthenticatorData != null)
            response.put("authenticatorData", webAuthnResponseAuthenticatorData);
        if (webAuthnResponseSignature != null)
            response.put("signature", webAuthnResponseSignature);
        if (webAuthnResponseUserHandle != null)
            response.put("userHandle", webAuthnResponseUserHandle);
    }
}
