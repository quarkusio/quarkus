package io.quarkus.oidc.runtime;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.IdTokenCredential;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpSecurityUtils;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

abstract class AbstractOidcAuthenticationMechanism {
    /**
     * System property key that is resolved to true if OIDC auth mechanism should put
     * `TokenCredential` into Vert.x duplicated context.
     */
    private static final String OIDC_PROPAGATE_TOKEN_CREDENTIAL = "io.quarkus.oidc.runtime." +
            "AbstractOidcAuthenticationMechanism.PROPAGATE_TOKEN_CREDENTIAL_WITH_DUPLICATED_CTX";
    private static final String ERROR_MSG = "OIDC requires a safe (isolated) Vert.x sub-context for propagation of the '"
            + TokenCredential.class.getName() + "', but the current context hasn't been flagged as such.";
    protected DefaultTenantConfigResolver resolver;
    /**
     * Propagate {@link TokenCredential} via Vert.X duplicated context if explicitly enabled and request context
     * can not be activated.
     */
    private final boolean propagateTokenCredentialWithDuplicatedCtx;
    private HttpAuthenticationMechanism parent;

    AbstractOidcAuthenticationMechanism() {
        // we use system property in order to keep this option internal and avoid introducing SPI
        this.propagateTokenCredentialWithDuplicatedCtx = Boolean
                .getBoolean(OIDC_PROPAGATE_TOKEN_CREDENTIAL);
    }

    protected Uni<SecurityIdentity> authenticate(IdentityProviderManager identityProviderManager,
            RoutingContext context, TokenCredential token) {
        context.put(HttpAuthenticationMechanism.class.getName(), parent);

        if (propagateTokenCredentialWithDuplicatedCtx) {
            // during authentication TokenCredential is not accessible via CDI, thus we put it to the duplicated context
            VertxContextSafetyToggle.validateContextIfExists(ERROR_MSG, ERROR_MSG);
            final var ctx = Vertx.currentContext();
            // If the primary token is ID token then the code flow access token is available as
            // a RoutingContext `access_token` property.
            final var tokenCredential = (token instanceof IdTokenCredential)
                    ? new AccessTokenCredential(context.get(OidcConstants.ACCESS_TOKEN_VALUE))
                    : token;
            ctx.putLocal(TokenCredential.class.getName(), tokenCredential);
            return identityProviderManager
                    .authenticate(HttpSecurityUtils.setRoutingContextAttribute(new TokenAuthenticationRequest(token), context))
                    .invoke(new Runnable() {
                        @Override
                        public void run() {
                            // remove as we recommend to acquire TokenCredential via CDI
                            ctx.removeLocal(TokenCredential.class.getName());
                        }
                    });
        }

        return identityProviderManager.authenticate(HttpSecurityUtils.setRoutingContextAttribute(
                new TokenAuthenticationRequest(token), context));
    }

    void init(HttpAuthenticationMechanism parent, DefaultTenantConfigResolver resolver) {
        this.parent = parent;
        this.resolver = resolver;
    }

}
