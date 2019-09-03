package io.quarkus.vertx.web.runtime;

import io.quarkus.security.identity.SecurityIdentity;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;

/**
 * Basic vert.x user representation
 */
public class QuarkusHttpUser implements User {

    private final SecurityIdentity securityIdentity;

    public QuarkusHttpUser(SecurityIdentity securityIdentity) {
        this.securityIdentity = securityIdentity;
    }

    @Override
    public User isAuthorized(String authority, Handler<AsyncResult<Boolean>> resultHandler) {
        resultHandler.handle(Future.succeededFuture(securityIdentity.getRoles().contains(authority)));
        return this;
    }

    @Override
    public User isAuthorised(String authority, Handler<AsyncResult<Boolean>> resultHandler) {
        resultHandler.handle(Future.succeededFuture(securityIdentity.getRoles().contains(authority)));
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
}
