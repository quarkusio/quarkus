package io.quarkus.produi.runtime;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

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

        // Record the application's main HTTP interface so the UI can link to it. Prod UI runs on the management
        // interface, so links must target the main port, not the port the UI page is served from. Only the
        // non-sensitive port and root path are exposed; the browser supplies the host.
        Config config = ConfigProvider.getConfig();
        Integer port = config.getOptionalValue("quarkus.http.port", Integer.class).orElse(null);
        String rootPath = config.getOptionalValue("quarkus.http.root-path", String.class).orElse("/");
        service.setHttpInfo(port, rootPath);
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
