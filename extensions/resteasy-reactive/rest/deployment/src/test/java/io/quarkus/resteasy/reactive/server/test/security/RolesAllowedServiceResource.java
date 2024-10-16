package io.quarkus.resteasy.reactive.server.test.security;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.Vertx;
import io.vertx.mutiny.core.eventbus.EventBus;
import io.vertx.mutiny.core.eventbus.MessageConsumer;

@Path("/roles-service")
public class RolesAllowedServiceResource {

    private MessageConsumer<String> permitAllConsumer;
    private MessageConsumer<String> rolesAllowedConsumer;

    @Inject
    RolesAllowedService rolesAllowedService;

    @Inject
    EventBus bus;

    @Path("/hello")
    @RolesAllowed({ "user", "admin" })
    @GET
    public String getServiceHello() {
        return rolesAllowedService.hello();
    }

    @Path("/bye")
    @GET
    public String getServiceBye() {
        return rolesAllowedService.bye();
    }

    @Path("/secured-event-bus")
    @POST
    public void sendMessage(String message) {
        bus.send("roles-allowed-message", message);
        bus.send("permit-all-message", message);
    }

    void observeStartup(@Observes StartupEvent startupEvent, EventBus eventBus, Vertx vertx) {
        permitAllConsumer = eventBus
                .<String> consumer("permit-all-message")
                .handler(msg -> rolesAllowedService.receivePermitAllMessage(msg.body()));

        // this must always fail because the authorization is happening in a blank CDI request context
        rolesAllowedConsumer = eventBus
                .<String> consumer("roles-allowed-message")
                .handler(msg -> vertx.executeBlocking(() -> {
                    // make sure authentication is attempted on a worker thread to prevent blocking event loop
                    rolesAllowedService.receiveRolesAllowedMessage(msg.body());
                    return null;
                }));
    }

    void observerShutdown(@Observes ShutdownEvent shutdownEvent) {
        if (permitAllConsumer != null) {
            permitAllConsumer.unregister().await().indefinitely();
        }
        if (rolesAllowedConsumer != null) {
            rolesAllowedConsumer.unregister().await().indefinitely();
        }
    }
}
