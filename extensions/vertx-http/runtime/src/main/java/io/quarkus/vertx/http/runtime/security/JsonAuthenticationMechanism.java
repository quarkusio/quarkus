package io.quarkus.vertx.http.runtime.security;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.jboss.logging.Logger;

import io.quarkus.security.credential.PasswordCredential;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.identity.request.TrustedAuthenticationRequest;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class JsonAuthenticationMechanism implements HttpAuthenticationMechanism {

    private static final Logger log = Logger.getLogger(JsonAuthenticationMechanism.class);

    private final DefaultPersistentLoginManager loginManager;
    private final String postLocation;

    public JsonAuthenticationMechanism(DefaultPersistentLoginManager loginManager, String postLocation) {
        this.loginManager = loginManager;
        this.postLocation = postLocation;
    }

    public Uni<SecurityIdentity> runJsonAuth(final RoutingContext exchange,
            final IdentityProviderManager securityContext) {
        JsonObject body = exchange.getBodyAsJson();
        if (body == null) {
            return Uni.createFrom().nullItem();
        }
        String username = body.getString("username");
        String password = body.getString("password");

        return Uni.createFrom().emitter(new Consumer<UniEmitter<? super SecurityIdentity>>() {
            @Override
            public void accept(UniEmitter<? super SecurityIdentity> uniEmitter) {
                securityContext
                        .authenticate(new UsernamePasswordAuthenticationRequest(username,
                                new PasswordCredential(password.toCharArray())))
                        .subscribe().with(new Consumer<SecurityIdentity>() {
                            @Override
                            public void accept(SecurityIdentity securityIdentity) {
                                loginManager.save(securityIdentity, exchange, null);
                                exchange.response().setStatusCode(200);
                                exchange.response().end();
                                uniEmitter.complete(null);
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) {
                                uniEmitter.fail(throwable);
                            }
                        });
            }
        });
    }

    @Override
    public Uni<SecurityIdentity> authenticate(RoutingContext context,
            IdentityProviderManager identityProviderManager) {

        DefaultPersistentLoginManager.RestoreResult result = loginManager.restore(context);
        if (result != null) {
            Uni<SecurityIdentity> ret = identityProviderManager
                    .authenticate(new TrustedAuthenticationRequest(result.getPrincipal()));
            return ret.onItem().invoke(new Consumer<SecurityIdentity>() {
                @Override
                public void accept(SecurityIdentity securityIdentity) {
                    loginManager.save(securityIdentity, context, result);
                }
            });
        }

        if (context.normalisedPath().endsWith(postLocation) && context.request().method().equals(HttpMethod.POST)) {
            return runJsonAuth(context, identityProviderManager);
        } else {
            return Uni.createFrom().optional(Optional.empty());
        }
    }

    @Override
    public Uni<ChallengeData> getChallenge(RoutingContext context) {
        return Uni.createFrom().item(new ChallengeData(401, null, null));
    }

    @Override
    public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
        return new HashSet<>(Arrays.asList(UsernamePasswordAuthenticationRequest.class, TrustedAuthenticationRequest.class));
    }

    @Override
    public HttpCredentialTransport getCredentialTransport() {
        return new HttpCredentialTransport(HttpCredentialTransport.Type.POST, postLocation);
    }
}
