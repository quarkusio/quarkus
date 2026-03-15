package io.quarkus.it.keycloak;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.common.OidcEndpoint;
import io.quarkus.oidc.common.OidcEndpoint.Type;
import io.quarkus.oidc.common.OidcRequestContextProperties;
import io.quarkus.oidc.common.OidcRequestFilter;
import io.quarkus.oidc.common.OidcResponseFilter;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.oidc.runtime.OidcUtils;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.buffer.Buffer;

@ApplicationScoped
@Unremovable
@OidcEndpoint(value = Type.TOKEN)
public class TokenRequestResponseFilter implements OidcRequestFilter, OidcResponseFilter {
    private static final Logger LOG = Logger.getLogger(TokenRequestResponseFilter.class);

    private ConcurrentHashMap<String, Instant> instants = new ConcurrentHashMap<>();

    @Override
    public void filter(OidcRequestContext rc) {
        final Instant now = Instant.now();
        String tenantId = rc.contextProperties().get(OidcUtils.TENANT_ID_ATTRIBUTE);
        instants.put(tenantId, now);
        rc.contextProperties().put("instant", now);
        if ("code-flow-opaque-access-token".equals(tenantId) || "github-no-id-token-no-user-info".equals(tenantId)) {
            rc.contextProperties().put(OidcRequestContextProperties.REQUEST_BODY,
                    Buffer.buffer(rc.requestBody().toString() + "&opaque_token_param=opaque_token_value"));
        }
    }

    @Override
    public void filter(OidcResponseContext rc) {
        Instant instant1 = instants.remove(rc.requestProperties().get(OidcUtils.TENANT_ID_ATTRIBUTE));
        Instant instant2 = rc.requestProperties().get("instant");
        boolean instantsAreTheSame = instant1 == instant2;
        if (rc.statusCode() == 200
                && instantsAreTheSame
                && rc.responseHeaders().get("Content-Type").equals("application/json")
                && OidcConstants.AUTHORIZATION_CODE.equals(rc.requestProperties().get(OidcConstants.GRANT_TYPE))
                && "code-flow-user-info-github-cached-in-idtoken"
                        .equals(rc.requestProperties().get(OidcUtils.TENANT_ID_ATTRIBUTE))) {
            LOG.debug("Authorization code completed for tenant 'code-flow-user-info-github-cached-in-idtoken' in an instant: "
                    + instantsAreTheSame);
        }
        if (rc.requestProperties().get(OidcRequestContextProperties.REQUEST_BODY) != null) {
            // Only the code-flow-opaque-access-token request customizes the request body
            JsonObject body = rc.responseBody().toJsonObject();
            String scope = body.getString("scope");
            body.put("scope", scope.replace(",", " "));
            rc.requestProperties().put(OidcRequestContextProperties.RESPONSE_BODY,
                    Buffer.buffer(body.toString()));
        }
    }

}
