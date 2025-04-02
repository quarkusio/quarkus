package io.quarkus.it.compose.devservices.rabbitmq;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.quarkus.arc.profile.IfBuildProfile;
import io.smallrye.reactive.messaging.MutinyEmitter;

@IfBuildProfile("rabbit")
@Path("/amqp")
public class AMQPEndpoint {

    List<String> received = new CopyOnWriteArrayList<>();

    @Incoming("test")
    public void consume(String message) {
        received.add(message);
    }

    @Channel("out")
    MutinyEmitter<String> emitter;

    @POST
    @Path("/send")
    public void send(String message) {
        emitter.sendAndAwait(message);
    }

    @GET
    @Path("/received")
    public List<String> received() {
        return received;
    }
}
