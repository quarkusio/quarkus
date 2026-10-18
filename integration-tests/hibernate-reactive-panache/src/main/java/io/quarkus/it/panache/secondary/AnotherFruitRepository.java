package io.quarkus.it.panache.secondary;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;

@ApplicationScoped
public class AnotherFruitRepository implements PanacheRepositoryBase<AnotherFruit, Long> {

}
