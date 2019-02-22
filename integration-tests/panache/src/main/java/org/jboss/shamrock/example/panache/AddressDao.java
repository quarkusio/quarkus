package org.jboss.shamrock.example.panache;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.shamrock.panache.jpa.PanacheRepository;

@ApplicationScoped
public class AddressDao implements PanacheRepository<Address> {

}
