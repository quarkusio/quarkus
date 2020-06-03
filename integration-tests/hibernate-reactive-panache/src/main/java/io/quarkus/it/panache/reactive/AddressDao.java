package io.quarkus.it.panache.reactive;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;

@ApplicationScoped
public class AddressDao implements PanacheRepository<Address> {

}
