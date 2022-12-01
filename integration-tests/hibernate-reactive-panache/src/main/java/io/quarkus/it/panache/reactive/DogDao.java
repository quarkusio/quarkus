package io.quarkus.it.panache.reactive;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;

// custom id type
@ApplicationScoped
public class DogDao implements PanacheRepositoryBase<Dog, Integer> {

}
