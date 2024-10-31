package io.quarkus.it.keycloak;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.common.OidcEndpoint;
import io.quarkus.oidc.common.OidcEndpoint.Type;
import io.quarkus.oidc.common.OidcRequestFilter;
import io.quarkus.oidc.common.OidcResponseFilter;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.oidc.runtime.OidcUtils;

@ApplicationScoped
@Unremovable
@OidcEndpoint(value = Type.TOKEN)
public class TokenRequestResponseFilter implements OidcRequestFilter, OidcResponseFilter {
    private static final Logger LOG = Logger.getLogger(TokenRequestResponseFilter.class);

    private ConcurrentHashMap<String, Instant> instants = new ConcurrentHashMap<>();

    @Override
    public void filter(OidcRequestContext rc) {
        final Instant now = Instant.now();
        instants.put(rc.contextProperties().get(OidcUtils.TENANT_ID_ATTRIBUTE), now);
        rc.contextProperties().put("instant", now);
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
    }

}
