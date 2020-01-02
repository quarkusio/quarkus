package io.quarkus.oidc.runtime;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.auth.oauth2.AccessToken;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class OidcIdentityProvider implements IdentityProvider<TokenAuthenticationRequest> {

    @Inject
    DefaultTenantConfigResolver tenantResolver;

    @Override
    public Class<TokenAuthenticationRequest> getRequestType() {
        return TokenAuthenticationRequest.class;
    }

    @SuppressWarnings("deprecation")
    @Override
    public CompletionStage<SecurityIdentity> authenticate(TokenAuthenticationRequest request,
            AuthenticationRequestContext context) {
        return context.runBlocking(new Supplier<SecurityIdentity>() {
            @Override
            public SecurityIdentity get() {
                CompletableFuture<SecurityIdentity> result = new CompletableFuture<>();
                ContextAwareTokenCredential credential = (ContextAwareTokenCredential) request.getToken();
                RoutingContext vertxContext = credential.getContext();

                tenantResolver.resolve(vertxContext).auth.decodeToken(request.getToken().getToken(),
                        new Handler<AsyncResult<AccessToken>>() {
                            @Override
                            public void handle(AsyncResult<AccessToken> event) {
                                if (event.failed()) {
                                    result.completeExceptionally(new AuthenticationFailedException());
                                    return;
                                }
                                AccessToken token = event.result();
                                try {
                                    OidcUtils.validateClaims(tenantResolver.resolve(vertxContext).oidcConfig.getToken(),
                                            token.accessToken());
                                } catch (OIDCException e) {
                                    result.completeExceptionally(new AuthenticationFailedException(e));
                                    return;
                                }

                                QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder();

                                JsonWebToken jwtPrincipal;
                                try {
                                    jwtPrincipal = new OidcJwtCallerPrincipal(JwtClaims.parse(token.accessToken().encode()));
                                } catch (InvalidJwtException e) {
                                    result.completeExceptionally(new AuthenticationFailedException(e));
                                    return;
                                }
                                builder.setPrincipal(jwtPrincipal);
                                try {
                                    String clientId = tenantResolver.resolve(vertxContext).oidcConfig.getClientId().isPresent()
                                            ? tenantResolver.resolve(vertxContext).oidcConfig.getClientId().get()
                                            : null;
                                    for (String role : OidcUtils.findRoles(clientId,
                                            tenantResolver.resolve(vertxContext).oidcConfig.getRoles(), token.accessToken())) {
                                        builder.addRole(role);
                                    }
                                } catch (Exception e) {
                                    result.completeExceptionally(new ForbiddenException(e));
                                    return;
                                }

                                builder.addCredential(request.getToken());
                                result.complete(builder.build());
                            }
                        });

                return result.join();
            }
        });
    }
}
