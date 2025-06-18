package io.quarkus.websockets.next.runtime;

import java.util.Map;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.websockets.next.HttpUpgradeCheck;
import io.quarkus.websockets.next.UserData;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

record HttpUpgradeContextImpl(RoutingContext routingContext, UserData userData,
        Uni<SecurityIdentity> securityIdentity, String endpointId) implements HttpUpgradeCheck.HttpUpgradeContext {

    @Override
    public Map<String, String> pathParams() {
        return routingContext.pathParams();
    }

    @Override
    public HttpServerRequest httpRequest() {
        return routingContext.request();
    }
}
