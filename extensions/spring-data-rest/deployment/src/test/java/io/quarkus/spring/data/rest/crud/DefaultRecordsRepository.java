package io.quarkus.spring.data.rest.crud;

import org.springframework.data.repository.CrudRepository;

public interface DefaultRecordsRepository extends CrudRepository<Record, Long> {
}
