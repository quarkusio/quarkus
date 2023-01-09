package io.quarkus.security.webauthn;

import java.util.function.Consumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.auth.User;

/**
 * WebAuthn IdentityProvider
 */
@ApplicationScoped
public class WebAuthnIdentityProvider implements IdentityProvider<WebAuthnAuthenticationRequest> {

    @Inject
    WebAuthnSecurity security;

    @Override
    public Class<WebAuthnAuthenticationRequest> getRequestType() {
        return WebAuthnAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(WebAuthnAuthenticationRequest request, AuthenticationRequestContext context) {
        return Uni.createFrom().emitter(new Consumer<UniEmitter<? super SecurityIdentity>>() {
            @Override
            public void accept(UniEmitter<? super SecurityIdentity> emitter) {
                security.getWebAuthn().authenticate(request.getCredentials(), new Handler<AsyncResult<User>>() {
                    @Override
                    public void handle(AsyncResult<User> event) {
                        if (event.failed()) {
                            emitter.fail(event.cause());
                        } else {
                            QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder();
                            // only the username matters, because when we auth we create a session cookie with it
                            // and we reply instantly so the roles are never used
                            builder.setPrincipal(new QuarkusPrincipal(request.getCredentials().getUsername()));
                            emitter.complete(builder.build());
                        }
                    }
                });
            }
        });
    }

}
