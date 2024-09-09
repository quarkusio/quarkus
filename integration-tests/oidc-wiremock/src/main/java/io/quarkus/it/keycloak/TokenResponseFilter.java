package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.common.OidcEndpoint;
import io.quarkus.oidc.common.OidcEndpoint.Type;
import io.quarkus.oidc.common.OidcResponseFilter;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.oidc.runtime.OidcUtils;

@ApplicationScoped
@Unremovable
@OidcEndpoint(value = Type.TOKEN)
public class TokenResponseFilter implements OidcResponseFilter {
    private static final Logger LOG = Logger.getLogger(TokenResponseFilter.class);

    @Override
    public void filter(OidcResponseContext rc) {
        if (rc.statusCode() == 200
                && rc.responseHeaders().get("Content-Type").equals("application/json")
                && OidcConstants.AUTHORIZATION_CODE.equals(rc.requestProperties().get(OidcConstants.GRANT_TYPE))
                && "code-flow-user-info-github-cached-in-idtoken"
                        .equals(rc.requestProperties().get(OidcUtils.TENANT_ID_ATTRIBUTE))) {
            LOG.debug("Authorization code completed for tenant 'code-flow-user-info-github-cached-in-idtoken'");
        }
    }

}
