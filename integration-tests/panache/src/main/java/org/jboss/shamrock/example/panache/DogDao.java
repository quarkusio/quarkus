package org.jboss.shamrock.example.panache;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.shamrock.panache.jpa.PanacheRepositoryBase;

// custom id type
@ApplicationScoped
public class DogDao implements PanacheRepositoryBase<Dog, Integer>{

}
