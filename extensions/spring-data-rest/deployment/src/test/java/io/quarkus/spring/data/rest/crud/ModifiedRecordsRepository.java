package io.quarkus.spring.data.rest.crud;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

@RepositoryRestResource(exported = false, path = "secret", collectionResourceRel = "secret-records")
public interface ModifiedRecordsRepository extends CrudRepository<Record, Long> {

    @Override
    @RestResource(path = "records")
    Iterable<Record> findAll();

    @Override
    @RestResource(path = "records")
    Optional<Record> findById(Long id);
}
