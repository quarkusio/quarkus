package io.quarkus.example.panache;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.panache.jpa.DaoBase;

@ApplicationScoped
public class AddressDao implements DaoBase<Address>{

}
