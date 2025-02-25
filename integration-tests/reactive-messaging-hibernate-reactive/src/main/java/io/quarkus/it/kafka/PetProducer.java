package io.quarkus.it.kafka;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.reactive.messaging.Channel;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.kafka.transactions.KafkaTransactions;

@Path("/kafka")
public class PetProducer {

    @Channel("pets")
    KafkaTransactions<Pet> petTransactions;

    @POST
    @Path("/pets")
    @WithTransaction
    public Uni<Void> createPet(String name) {
        Pet p = new Pet(name);
        return petTransactions.withTransaction(e -> p.persist()
                .map(pet -> (Pet) pet)
                .invoke(pet -> {
                    if (pet.name.equals("bad")) {
                        throw new IllegalArgumentException("bad pet");
                    }
                })
                .invoke(pet -> Log.infov("Persisted {0}", pet))
                .invoke(e::send)
                .replaceWithVoid())
                .onFailure().call(f -> {
                    // apply any cleanup to do
                    return Uni.createFrom().voidItem();
                });
    }
}
