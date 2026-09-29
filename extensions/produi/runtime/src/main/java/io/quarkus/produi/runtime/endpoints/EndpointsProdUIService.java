package io.quarkus.produi.runtime.endpoints;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.JsonRpcUsage;
import io.quarkus.runtime.annotations.Usage;
import io.smallrye.common.annotation.NonBlocking;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class EndpointsProdUIService {

    private volatile JsonArray endpoints = new JsonArray();
    private volatile JsonObject httpInfo = new JsonObject().put("rootPath", "/");

    public void setEndpoints(JsonArray endpoints) {
        this.endpoints = endpoints;
    }

    /**
     * Records the application's main HTTP interface so the UI can build links to it. Prod UI is served on the
     * management interface (a different port), so a root-relative link from the UI page would resolve against the
     * management port. The UI combines this port and root path with the browser's own host to build a correct link.
     * Both values are non-sensitive.
     */
    public void setHttpInfo(Integer port, String rootPath) {
        JsonObject info = new JsonObject();
        if (port != null) {
            info.put("port", port);
        }
        info.put("rootPath", rootPath == null || rootPath.isEmpty() ? "/" : rootPath);
        this.httpInfo = info;
    }

    @NonBlocking
    @JsonRpcUsage({ Usage.PROD_UI })
    @JsonRpcDescription("Get all registered endpoints")
    public JsonArray getAllRoutes() {
        return endpoints;
    }

    @NonBlocking
    @JsonRpcUsage({ Usage.PROD_UI })
    @JsonRpcDescription("Get the application HTTP interface (port and root path) used to build endpoint links")
    public JsonObject getHttpInfo() {
        return httpInfo;
    }
}
