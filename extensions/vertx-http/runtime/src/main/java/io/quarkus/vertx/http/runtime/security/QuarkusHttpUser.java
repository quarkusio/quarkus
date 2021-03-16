package io.quarkus.vertx.http.runtime.security;

import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AnonymousAuthenticationRequest;
import io.smallrye.mutiny.Uni;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;

/**
 * Basic vert.x user representation
 */
public class QuarkusHttpUser implements User {

    /**
     * Only used when proactive auth is disabled
     */
    public static final String DEFERRED_IDENTITY_KEY = "io.quarkus.vertx.http.deferred-identity";
    /**
     * The key that stores a BiConsumer that handles auth failures
     *
     * This can be overriden by downstream handlers such as Undertow to control auth failure handling.
     */
    public static final String AUTH_FAILURE_HANDLER = "io.quarkus.vertx.http.auth-failure-handler";

    private final SecurityIdentity securityIdentity;

    public QuarkusHttpUser(SecurityIdentity securityIdentity) {
        this.securityIdentity = securityIdentity;
    }

    @Override
    public User isAuthorized(String authority, Handler<AsyncResult<Boolean>> resultHandler) {
        resultHandler.handle(Future.succeededFuture(securityIdentity.hasRole(authority)));
        return this;
    }

    @Override
    public User isAuthorised(String authority, Handler<AsyncResult<Boolean>> resultHandler) {
        resultHandler.handle(Future.succeededFuture(securityIdentity.hasRole(authority)));
        return this;
    }

    @Override
    public User clearCache() {
        return this;
    }

    @Override
    public JsonObject principal() {
        JsonObject ret = new JsonObject();
        ret.put("username", securityIdentity.getPrincipal().getName());
        return ret;
    }

    @Override
    public void setAuthProvider(AuthProvider authProvider) {

    }

    public SecurityIdentity getSecurityIdentity() {
        return securityIdentity;
    }

    /**
     * Gets the current user from the routing context. This method may block if proactive authentication is disabled,
     * as it may need to perform a potentially blocking operation.
     * If an IPM is provided this method will return the anonymous
     * identity if there is no active user, otherwise it will return null if there is no user.
     */
    public static SecurityIdentity getSecurityIdentityBlocking(RoutingContext routingContext,
            IdentityProviderManager identityProviderManager) {
        QuarkusHttpUser existing = (QuarkusHttpUser) routingContext.user();
        if (existing != null) {
            return existing.getSecurityIdentity();
        }
        Uni<SecurityIdentity> deferred = routingContext.get(DEFERRED_IDENTITY_KEY);
        if (deferred != null) {
            return deferred.await().indefinitely();
        }
        if (identityProviderManager != null) {
            return identityProviderManager.authenticate(AnonymousAuthenticationRequest.INSTANCE).await().indefinitely();
        }
        return null;
    }

    /**
     * Gets the current user from the routing context. If an IPM is provided this method will return the anonymous
     * identity if there is no active user, otherwise the Uni will resolve to null if there is no user.
     */
    public static Uni<SecurityIdentity> getSecurityIdentity(RoutingContext routingContext,
            IdentityProviderManager identityProviderManager) {
        Uni<SecurityIdentity> deferred = routingContext.get(DEFERRED_IDENTITY_KEY);
        if (deferred != null) {
            return deferred;
        }
        QuarkusHttpUser existing = (QuarkusHttpUser) routingContext.user();
        if (existing != null) {
            return Uni.createFrom().item(existing.getSecurityIdentity());
        }
        if (identityProviderManager != null) {
            return identityProviderManager.authenticate(AnonymousAuthenticationRequest.INSTANCE);
        }
        return Uni.createFrom().nullItem();
    }
}
