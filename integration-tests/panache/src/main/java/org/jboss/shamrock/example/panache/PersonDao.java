package org.jboss.shamrock.example.panache;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.panache.DaoBase;

@ApplicationScoped
public class PersonDao implements DaoBase<Person> {
}
