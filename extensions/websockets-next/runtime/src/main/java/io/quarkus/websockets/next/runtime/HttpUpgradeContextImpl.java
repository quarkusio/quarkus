package io.quarkus.websockets.next.runtime;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.websockets.next.HttpUpgradeCheck;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

record HttpUpgradeContextImpl(RoutingContext routingContext,
        Uni<SecurityIdentity> securityIdentity, String endpointId) implements HttpUpgradeCheck.HttpUpgradeContext {

    @Override
    public HttpServerRequest httpRequest() {
        return routingContext.request();
    }
}
