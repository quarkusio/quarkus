package io.quarkus.it.spring.data.rest;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RestResource;

public interface AuthorsRepository extends CrudRepository<Author, Long> {

    @RestResource(exported = false)
    <S extends Author> S save(S author);

    @RestResource(exported = false)
    void deleteById(Long id);
}
