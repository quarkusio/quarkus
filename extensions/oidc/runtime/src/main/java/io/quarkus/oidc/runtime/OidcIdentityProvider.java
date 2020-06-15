package io.quarkus.oidc.runtime;

import static io.quarkus.oidc.runtime.OidcUtils.validateAndCreateIdentity;

import java.security.Principal;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.IdTokenCredential;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.credential.TokenCredential;
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
        return Uni.createFrom().deferred(new Supplier<Uni<SecurityIdentity>>() {
            @Override
            public Uni<SecurityIdentity> get() {
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
        });

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
                                // Token has been verified, as a JWT or an opaque token, possibly involving
                                // an introspection request.
                                final TokenCredential tokenCred = request.getToken();
                                JsonObject tokenJson = event.result().accessToken();
                                if (tokenJson == null) {
                                    // JSON token representation may be null not only if it is an opaque access token
                                    // but also if it is JWT and no JWK with a matching kid is available, asynchronous
                                    // JWK refresh has not finished yet, but the fallback introspection request has succeeded.
                                    tokenJson = OidcUtils.decodeJwtContent(tokenCred.getToken());
                                }
                                if (tokenJson != null) {
                                    try {
                                        uniEmitter.complete(
                                                validateAndCreateIdentity(tokenCred, resolvedContext.oidcConfig, tokenJson));
                                    } catch (Throwable ex) {
                                        uniEmitter.fail(ex);
                                    }
                                } else if (tokenCred instanceof IdTokenCredential
                                        || tokenCred instanceof AccessTokenCredential
                                                && !((AccessTokenCredential) tokenCred).isOpaque()) {
                                    uniEmitter
                                            .fail(new AuthenticationFailedException("JWT token can not be converted to JSON"));
                                } else {
                                    // Opaque access token
                                    QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder();
                                    builder.addCredential(tokenCred);
                                    if (event.result().principal().containsKey("username")) {
                                        final String userName = event.result().principal().getString("username");
                                        builder.setPrincipal(new Principal() {
                                            @Override
                                            public String getName() {
                                                return userName;
                                            }
                                        });
                                    }
                                    uniEmitter.complete(builder.build());
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
                return Uni.createFrom()
                        .item(validateAndCreateIdentity(request.getToken(), resolvedContext.oidcConfig, tokenJson));
            } catch (Throwable ex) {
                return Uni.createFrom().failure(ex);
            }
        }
    }
}
