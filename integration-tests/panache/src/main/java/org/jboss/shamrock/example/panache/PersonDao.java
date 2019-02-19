package org.jboss.shamrock.example.panache;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.panache.jpa.DaoBase;

@ApplicationScoped
public class PersonDao implements DaoBase<Person> {
}
