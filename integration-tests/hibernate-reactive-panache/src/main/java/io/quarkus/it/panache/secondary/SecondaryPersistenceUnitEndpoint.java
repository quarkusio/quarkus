package io.quarkus.it.panache.secondary;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.it.panache.reactive.Fruit;
import io.quarkus.it.panache.reactive.FruitRepository;
import io.smallrye.mutiny.Uni;

@Path("secondaryPersistenceUnit")
public class SecondaryPersistenceUnitEndpoint {

    @Inject
    AnotherFruitRepository anotherFruitRepository;

    @Inject
    FruitRepository fruitRepository;

    @WithTransaction
    @GET
    public Uni<String> testModel() {

        Fruit fruit = new Fruit("mainFruit", "mainColor");
        AnotherFruit anotherFruit = new AnotherFruit("secondaryFruit", "secondaryColor");

        return fruitRepository.persist(fruit)
                .flatMap(f -> anotherFruitRepository.persist(anotherFruit)
                        .flatMap(a -> {
                            Uni<Fruit> fetchedFruit = fruitRepository.find("color", f.color).firstResult();
                            Uni<AnotherFruit> fetchedAnother = anotherFruitRepository.find("color", a.color).firstResult();

                            return Uni.combine().all().unis(fetchedFruit, fetchedAnother).asTuple()
                                    .map(tuple -> tuple.getItem1().color + " " + tuple.getItem2().color);
                        }));

    }

}
