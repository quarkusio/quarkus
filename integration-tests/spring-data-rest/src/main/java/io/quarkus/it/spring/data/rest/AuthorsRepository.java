package io.quarkus.it.spring.data.rest;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RestResource;

@PermitAll
public interface AuthorsRepository extends CrudRepository<Author, Long> {

    @Override
    @RolesAllowed("user")
    Iterable<Author> findAll();

    @RestResource(exported = false)
    @RolesAllowed("superuser")
    <S extends Author> S save(S author);

    @RestResource(exported = false)
    void deleteById(Long id);
}
