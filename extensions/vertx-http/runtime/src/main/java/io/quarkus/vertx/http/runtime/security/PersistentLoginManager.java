package io.quarkus.vertx.http.runtime.security;

import io.quarkus.security.identity.SecurityIdentity;
import io.vertx.ext.web.RoutingContext;

public interface PersistentLoginManager {
    DefaultPersistentLoginManager.RestoreResult restore(RoutingContext context);

    void save(SecurityIdentity identity, RoutingContext context, DefaultPersistentLoginManager.RestoreResult restoreResult);
}
