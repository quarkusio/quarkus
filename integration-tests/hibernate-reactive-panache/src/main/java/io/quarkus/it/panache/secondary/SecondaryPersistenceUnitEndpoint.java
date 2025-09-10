package io.quarkus.it.panache.secondary;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.hibernate.reactive.panache.common.WithSessionOnDemand;
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

    @GET
    @WithTransaction
    @WithSessionOnDemand
    /*
     * I need @WithSessionOnDemans explictly due to
     * io/quarkus/hibernate/reactive/panache/common/deployment/PanacheJpaCommonResourceProcessor.java:177
     * // Add @WithSessionOnDemand to a method that
     * ...
     * // - is not annotated with @ReactiveTransactional, @WithSession, @WithSessionOnDemand, or @WithTransaction
     * Because this method is annotated with @WithTransaction
     */
    public Uni<String> createEntitiesInTwoRepositories() {

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
