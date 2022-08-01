package io.quarkus.kafka.client.runtime.ui;

import java.util.List;
import java.util.function.Consumer;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.quarkus.vertx.http.runtime.devmode.FileSystemStaticHandler;
import io.quarkus.vertx.http.runtime.webjar.WebJarStaticHandler;
import io.vertx.core.Handler;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;

/**
 * Handles requests from kafka UI and html/js of UI
 */
@Recorder
public class KafkaUiRecorder {

    public Handler<RoutingContext> kafkaControlHandler() {
        return new KafkaUiHandler(getCurrentIdentityAssociation(),
                Arc.container().instance(CurrentVertxRequest.class).get());
    }

    public Consumer<Route> routeFunction(Handler<RoutingContext> bodyHandler) {
        return route -> route.handler(bodyHandler);
    }

    public Handler<RoutingContext> uiHandler(String finalDestination, String uiPath,
            List<FileSystemStaticHandler.StaticWebRootConfiguration> webRootConfigurations,
            ShutdownContext shutdownContext) {
        WebJarStaticHandler handler = new WebJarStaticHandler(finalDestination, uiPath, webRootConfigurations);
        shutdownContext.addShutdownTask(new ShutdownContext.CloseRunnable(handler));
        return handler;
    }

    private CurrentIdentityAssociation getCurrentIdentityAssociation() {
        InstanceHandle<CurrentIdentityAssociation> identityAssociations = Arc.container()
                .instance(CurrentIdentityAssociation.class);
        if (identityAssociations.isAvailable()) {
            return identityAssociations.get();
        }
        return null;
    }
}
