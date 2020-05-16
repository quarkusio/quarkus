package io.quarkus.it.hibernate.orm.rest.data.panache.repository;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.orm.panache.PanacheRepository;

@ApplicationScoped
public class AuthorPojoRepository implements PanacheRepository<AuthorPojo> {

}
