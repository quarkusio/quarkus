package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.common.OidcEndpoint;
import io.quarkus.oidc.common.OidcEndpoint.Type;
import io.quarkus.oidc.common.OidcRequestContextProperties;
import io.quarkus.oidc.common.OidcRequestFilter;
import io.quarkus.oidc.common.OidcResponseFilter;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;

@ApplicationScoped
@Unremovable
@OidcEndpoint(value = Type.TOKEN)
public class OidcRequestResponseCustomizer implements OidcRequestFilter, OidcResponseFilter {

    @Override
    public void filter(HttpRequest<Buffer> request, Buffer buffer, OidcRequestContextProperties contextProps) {
        String uri = request.uri();
        if (uri.endsWith("/non-standard-tokens")) {
            request.putHeader("client-id", contextProps.getString("client-id"));
            request.putHeader("GrantType", getGrantType(buffer.toString()));
            contextProps.put(OidcRequestContextProperties.REQUEST_BODY,
                    Buffer.buffer(buffer.toString() + "&custom_prop=custom_value"));
        }
    }

    private String getGrantType(String formString) {
        for (String formValue : formString.split("&")) {
            if (formValue.startsWith("grant_type=")) {
                return formValue.substring("grant_type=".length());
            }
        }
        return "";
    }

    @Override
    public void filter(OidcResponseContext responseContext) {
        if (responseContext.statusCode() == 200
                && responseContext.requestProperties().get(OidcRequestContextProperties.REQUEST_BODY) != null) {
            // Only the non-standards-tokens request customizes the request body
            JsonObject body = responseContext.responseBody().toJsonObject();
            body.put("refreshToken", "refresh_token_non_standard");
            responseContext.requestProperties().put(OidcRequestContextProperties.RESPONSE_BODY,
                    Buffer.buffer(body.toString()));
        }

    }
}
