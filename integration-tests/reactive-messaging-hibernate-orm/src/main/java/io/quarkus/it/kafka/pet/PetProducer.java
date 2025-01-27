package io.quarkus.it.kafka.pet;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.reactive.messaging.Channel;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.kafka.transactions.KafkaTransactions;

@Path("/kafka")
public class PetProducer {

    @Channel("pets")
    KafkaTransactions<Pet> emitter;

    @POST
    @Path("/pets")
    @Transactional
    public void post(String name) {
        Log.infov("Sending pet {0}", name);
        Pet pet = new Pet(name);
        emitter.withTransaction(e -> {
            pet.persist();
            if (pet.name.equals("bad")) {
                throw new IllegalArgumentException("bad pet");
            }
            Log.infov("Persisted pet {0}", pet);
            e.send(pet);
            return Uni.createFrom().voidItem();
        }).await().indefinitely();
    }
}
