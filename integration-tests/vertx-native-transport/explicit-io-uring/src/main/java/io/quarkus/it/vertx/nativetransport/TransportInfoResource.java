package io.quarkus.it.vertx.nativetransport;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.vertx.core.runtime.config.NativeTransportType;
import io.vertx.core.Vertx;
import io.vertx.core.impl.VertxImpl;
import io.vertx.core.json.JsonObject;

@Path("/transport")
public class TransportInfoResource {

    @Inject
    Vertx vertx;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getTransportInfo() {
        String type = detectActiveTransport();
        return new JsonObject()
                .put("type", type)
                .put("nativeTransportEnabled", vertx.isNativeTransportEnabled())
                .encode();
    }

    private String detectActiveTransport() {
        if (!vertx.isNativeTransportEnabled()) {
            return "nio";
        }
        // Check the actual Vert.x transport implementation, not just availability.
        // This matters when multiple native transports are on the classpath.
        String className = ((VertxImpl) vertx).transport().getClass().getSimpleName();
        if (className.contains("Epoll")) {
            return NativeTransportType.EPOLL.transportName;
        }
        if (className.contains("KQueue")) {
            return NativeTransportType.KQUEUE.transportName;
        }
        if (className.contains("IoUring")) {
            return NativeTransportType.IO_URING.transportName;
        }
        return "unknown";
    }
}
