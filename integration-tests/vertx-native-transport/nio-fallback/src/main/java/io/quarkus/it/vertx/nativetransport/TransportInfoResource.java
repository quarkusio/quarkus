package io.quarkus.it.vertx.nativetransport;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.logging.Logger;

import io.quarkus.vertx.core.runtime.config.NativeTransportType;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.transport.Transport;

@Path("/transport")
public class TransportInfoResource {

    @Inject
    Vertx vertx;

    private static final Logger log = Logger.getLogger(TransportInfoResource.class);

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
        try {
            if (Transport.EPOLL != null && Transport.EPOLL.available()) {
                return NativeTransportType.EPOLL.transportName;
            }
        } catch (Throwable ignored) {
            log.warn("Unable to load epoll transport type", vertx.unavailableNativeTransportCause());
        }
        try {
            if (Transport.KQUEUE != null && Transport.KQUEUE.available()) {
                return NativeTransportType.KQUEUE.transportName;
            }
        } catch (Throwable ignored) {
            log.warn("Unable to load kqueue transport type", vertx.unavailableNativeTransportCause());
        }
        try {
            if (Transport.IO_URING != null && Transport.IO_URING.available()) {
                return NativeTransportType.IO_URING.transportName;
            }
        } catch (Throwable ignored) {
            log.warn("Unable to load io_uring transport type", vertx.unavailableNativeTransportCause());
        }
        return "unknown";
    }
}
