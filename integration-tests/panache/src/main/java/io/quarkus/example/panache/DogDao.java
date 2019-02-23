package io.quarkus.example.panache;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.panache.jpa.PanacheRepositoryBase;

// custom id type
@ApplicationScoped
public class DogDao implements PanacheRepositoryBase<Dog, Integer> {

}
