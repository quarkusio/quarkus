package io.quarkus.produi.runtime.endpoints;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.JsonRpcUsage;
import io.quarkus.runtime.annotations.Usage;
import io.smallrye.common.annotation.NonBlocking;
import io.vertx.core.json.JsonArray;

@ApplicationScoped
public class EndpointsProdUIService {

    private volatile JsonArray endpoints = new JsonArray();

    public void setEndpoints(JsonArray endpoints) {
        this.endpoints = endpoints;
    }

    @NonBlocking
    @JsonRpcUsage({ Usage.PROD_UI })
    @JsonRpcDescription("Get all registered endpoints")
    public JsonArray getAllRoutes() {
        return endpoints;
    }
}
