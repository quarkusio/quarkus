package io.quarkus.it.hibernate.processor.data.pudefault;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Repository;

@Repository
public interface MyRepository extends CrudRepository<MyEntity, Long> {

}
