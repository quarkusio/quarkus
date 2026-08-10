package io.quarkus.produi.runtime;

import java.util.List;
import java.util.Map;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.devjsonrpc.runtime.comms.JsonRpcRouter;
import io.quarkus.devjsonrpc.runtime.jsonrpc.JsonRpcMethod;
import io.quarkus.produi.runtime.endpoints.EndpointsProdUIService;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;

@Recorder
public class ProdUIRecorder {

    public void initializeJsonRpcRouter(BeanContainer beanContainer,
            Map<String, JsonRpcMethod> runtimeMethods,
            Map<String, JsonRpcMethod> runtimeSubscriptions) {

        JsonRpcRouter jsonRpcRouter = beanContainer.beanInstance(JsonRpcRouter.class);
        jsonRpcRouter.initializeCodec(new VertxJsonMapper());
        jsonRpcRouter.populateJsonRpcEndpoints(
                runtimeMethods,
                runtimeSubscriptions,
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of());
    }

    public Handler<RoutingContext> prodUIWebSocketHandler() {
        return new ProdUIWebSocketHandler();
    }

    public Handler<RoutingContext> classpathStaticHandler(String webRoot) {
        return StaticHandler.create(webRoot);
    }

    public void registerEndpoints(BeanContainer beanContainer, List<String> endpoints) {
        EndpointsProdUIService service = beanContainer.beanInstance(EndpointsProdUIService.class);
        io.vertx.core.json.JsonArray array = new io.vertx.core.json.JsonArray();
        for (String endpoint : endpoints) {
            String[] parts = endpoint.split("\\|", 2);
            io.vertx.core.json.JsonObject entry = new io.vertx.core.json.JsonObject();
            entry.put("path", parts[0]);
            entry.put("methods", parts.length > 1 ? parts[1] : "*");
            array.add(entry);
        }
        service.setEndpoints(array);
    }

    public Handler<RoutingContext> spaFallbackHandler(String indexPath) {
        return ctx -> {
            String path = ctx.normalizedPath();
            if (path.contains(".")) {
                ctx.next();
            } else {
                ctx.reroute(indexPath);
            }
        };
    }
}
