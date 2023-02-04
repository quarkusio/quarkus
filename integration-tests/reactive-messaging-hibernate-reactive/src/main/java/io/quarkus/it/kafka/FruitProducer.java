package io.quarkus.it.kafka;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.reactive.messaging.Channel;

import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;

@Path("/kafka")
public class FruitProducer {

    @Channel("fruits-out")
    MutinyEmitter<Fruit> emitter;

    @POST
    @Path("/fruits")
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<Void> post(Fruit fruit) {
        assert VertxContext.isOnDuplicatedContext();
        return emitter.send(fruit);
    }
}
