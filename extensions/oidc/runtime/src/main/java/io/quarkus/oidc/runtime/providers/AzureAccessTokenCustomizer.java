package io.quarkus.oidc.runtime.providers;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.TokenCustomizer;
import io.quarkus.oidc.runtime.OidcUtils;

@Named("azure-access-token-customizer")
@ApplicationScoped
public class AzureAccessTokenCustomizer implements TokenCustomizer {
    private static final String NONCE = "nonce";

    @Override
    public JsonObject customizeHeaders(JsonObject headers) {
        try {
            String nonce = headers.getString(NONCE);
            if (nonce != null) {
                byte[] nonceSha256 = OidcUtils.getSha256Digest(nonce.getBytes(StandardCharsets.UTF_8));
                byte[] newNonceBytes = Base64.getUrlEncoder().withoutPadding().encode(nonceSha256);
                return Json.createObjectBuilder(headers)
                        .add(NONCE, new String(newNonceBytes, StandardCharsets.UTF_8)).build();
            }
            return null;
        } catch (Exception ex) {
            throw new OIDCException(ex);
        }
    }

}
