package io.quarkus.it.panache.hibernate;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

@ApplicationScoped
public class HibernateBookRepository implements PanacheRepositoryBase<HibernateBook, Integer> {
}
