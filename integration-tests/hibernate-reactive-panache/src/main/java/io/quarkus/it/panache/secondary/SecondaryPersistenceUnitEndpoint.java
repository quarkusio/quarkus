package io.quarkus.it.panache.secondary;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.common.WithSessionOnDemand;
import io.quarkus.it.panache.reactive.Fruit;
import io.quarkus.it.panache.reactive.FruitRepository;
import io.smallrye.mutiny.Uni;

@Path("secondaryPersistenceUnit")
public class SecondaryPersistenceUnitEndpoint {

    @Inject
    AnotherFruitRepository anotherFruitRepository;

    @Inject
    FruitRepository fruitRepository;

    @GET
    @WithSessionOnDemand
    public Uni<String> testWithSessionOnDemandOnTwoSessions() {

        Fruit fruit = new Fruit("mainFruit", "color");
        AnotherFruit anotherFruit = new AnotherFruit("secondaryFruit", "color");

        Uni<Fruit> persistDefaultTransaction = Panache.withTransaction(() -> fruitRepository.persist(fruit));
        Uni<AnotherFruit> persistAnotherTransaction = Panache.withTransaction("secondary",
                () -> anotherFruitRepository.persist(anotherFruit));

        return persistDefaultTransaction
                .flatMap(f1 -> persistAnotherTransaction)
                .flatMap(a -> findBothFruits(a.color));

    }

    private Uni<String> findBothFruits(String color) {
        Uni<Fruit> fetchedFruit = fruitRepository.find("color", color).firstResult();
        Uni<AnotherFruit> fetchedAnother = anotherFruitRepository.find("color", color).firstResult();

        return Uni.combine().all().unis(fetchedFruit, fetchedAnother).asTuple()
                .map(tuple -> tuple.getItem1().name + " " + tuple.getItem2().name);
    }

}
