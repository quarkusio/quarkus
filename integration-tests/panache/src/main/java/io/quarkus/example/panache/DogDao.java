package io.quarkus.example.panache;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.panache.jpa.DaoBase;

@ApplicationScoped
public class DogDao implements DaoBase<Dog>{

}
