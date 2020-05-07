package io.quarkus.it.panache.rest.repository;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.orm.panache.PanacheRepository;

@ApplicationScoped
public class BookPojoRepository implements PanacheRepository<BookPojo> {
}
