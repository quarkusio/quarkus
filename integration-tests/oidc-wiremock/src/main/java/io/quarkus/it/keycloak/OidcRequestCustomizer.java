package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.common.OidcRequestContextProperties;
import io.quarkus.oidc.common.OidcRequestFilter;
import io.vertx.core.http.HttpMethod;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;

@ApplicationScoped
@Unremovable
public class OidcRequestCustomizer implements OidcRequestFilter {

    @Override
    public void filter(HttpRequest<Buffer> request, Buffer buffer, OidcRequestContextProperties contextProps) {
        HttpMethod method = request.method();
        String uri = request.uri();
        if (method == HttpMethod.GET && uri.endsWith("/auth/azure/jwk")) {
            String token = contextProps.getString(OidcRequestContextProperties.TOKEN);
            AccessTokenCredential tokenCred = contextProps.get(OidcRequestContextProperties.TOKEN_CREDENTIAL,
                    AccessTokenCredential.class);
            // or
            // IdTokenCredential tokenCred = contextProps.get(OidcRequestContextProperties.TOKEN_CREDENTIAL,
            //                                                 IdTokenCredential.class);
            // or
            // TokenCredential tokenCred = contextProps.get(OidcRequestContextProperties.TOKEN_CREDENTIAL,
            //                                                 TokenCredential.class);
            // if either access or ID token has to be verified and check is it an instanceof
            // AccessTokenCredential or IdTokenCredential
            // or simply
            // String token = contextProps.getString(OidcRequestContextProperties.TOKEN);
            if (token.equals(tokenCred.getToken())) {
                request.putHeader("Authorization", "Access token: " + token);
            }
        }
    }

}
