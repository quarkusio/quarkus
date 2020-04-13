package io.quarkus.oidc.runtime;

import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;

import io.quarkus.oidc.OIDCException;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.AccessToken;
import io.vertx.ext.auth.oauth2.impl.OAuth2AuthProviderImpl;
import io.vertx.ext.jwt.JWT;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class OidcIdentityProvider implements IdentityProvider<TokenAuthenticationRequest> {
    @Inject
    DefaultTenantConfigResolver tenantResolver;

    @Override
    public Class<TokenAuthenticationRequest> getRequestType() {
        return TokenAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(TokenAuthenticationRequest request,
            AuthenticationRequestContext context) {
        ContextAwareTokenCredential credential = (ContextAwareTokenCredential) request.getToken();
        RoutingContext vertxContext = credential.getContext();

        if (tenantResolver.isBlocking(vertxContext)) {
            return context.runBlocking(new Supplier<SecurityIdentity>() {
                @Override
                public SecurityIdentity get() {
                    return authenticate(request, vertxContext).await().indefinitely();
                }
            });
        }

        return authenticate(request, vertxContext);
    }

    private Uni<SecurityIdentity> authenticate(TokenAuthenticationRequest request,
            RoutingContext vertxContext) {
        TenantConfigContext resolvedContext = tenantResolver.resolve(vertxContext, true);

        if (resolvedContext.oidcConfig.publicKey.isPresent()) {
            return validateTokenWithoutOidcServer(request, resolvedContext);
        } else {
            return validateTokenWithOidcServer(request, resolvedContext);
        }
    }

    @SuppressWarnings("deprecation")
    private Uni<SecurityIdentity> validateTokenWithOidcServer(TokenAuthenticationRequest request,
            TenantConfigContext resolvedContext) {

        return Uni.createFrom().emitter(new Consumer<UniEmitter<? super SecurityIdentity>>() {
            @Override
            public void accept(UniEmitter<? super SecurityIdentity> uniEmitter) {
                resolvedContext.auth.decodeToken(request.getToken().getToken(),
                        new Handler<AsyncResult<AccessToken>>() {
                            @Override
                            public void handle(AsyncResult<AccessToken> event) {
                                if (event.failed()) {
                                    uniEmitter.fail(new AuthenticationFailedException(event.cause()));
                                    return;
                                }
                                JsonObject tokenJson = event.result().accessToken();
                                try {
                                    uniEmitter.complete(
                                            validateAndCreateIdentity(request, resolvedContext.oidcConfig, tokenJson));
                                } catch (Throwable ex) {
                                    uniEmitter.fail(ex);
                                }
                            }
                        });
            }
        });
    }

    private Uni<SecurityIdentity> validateTokenWithoutOidcServer(TokenAuthenticationRequest request,
            TenantConfigContext resolvedContext) {
        OAuth2AuthProviderImpl auth = ((OAuth2AuthProviderImpl) resolvedContext.auth);
        JWT jwt = auth.getJWT();
        JsonObject tokenJson = null;
        try {
            tokenJson = jwt.decode(request.getToken().getToken());
        } catch (Throwable ex) {
            return Uni.createFrom().failure(new AuthenticationFailedException(ex));
        }
        if (jwt.isExpired(tokenJson, auth.getConfig().getJWTOptions())) {
            return Uni.createFrom().failure(new AuthenticationFailedException());
        } else {
            try {
                return Uni.createFrom().item(validateAndCreateIdentity(request, resolvedContext.oidcConfig, tokenJson));
            } catch (Throwable ex) {
                return Uni.createFrom().failure(ex);
            }
        }
    }

    private QuarkusSecurityIdentity validateAndCreateIdentity(TokenAuthenticationRequest request,
            OidcTenantConfig config, JsonObject tokenJson)
            throws Exception {
        try {
            OidcUtils.validateClaims(config.getToken(), tokenJson);
        } catch (OIDCException e) {
            throw new AuthenticationFailedException(e);
        }

        QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder();
        builder.addCredential(request.getToken());

        JsonWebToken jwtPrincipal;
        try {
            JwtClaims jwtClaims = JwtClaims.parse(tokenJson.encode());
            jwtClaims.setClaim(Claims.raw_token.name(), request.getToken().getToken());
            jwtPrincipal = new OidcJwtCallerPrincipal(jwtClaims, request.getToken(),
                    config.token.principalClaim.isPresent() ? config.token.principalClaim.get() : null);
        } catch (InvalidJwtException e) {
            throw new AuthenticationFailedException(e);
        }
        builder.setPrincipal(jwtPrincipal);
        try {
            String clientId = config.getClientId().isPresent() ? config.getClientId().get() : null;
            for (String role : OidcUtils.findRoles(clientId, config.getRoles(), tokenJson)) {
                builder.addRole(role);
            }
        } catch (Exception e) {
            throw new ForbiddenException(e);
        }
        return builder.build();
    }
}
