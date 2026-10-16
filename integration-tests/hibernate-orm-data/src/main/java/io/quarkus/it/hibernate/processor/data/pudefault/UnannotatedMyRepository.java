package io.quarkus.it.hibernate.processor.data.pudefault;

import java.util.List;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Query;

public interface UnannotatedMyRepository extends CrudRepository<MyEntity, Integer> {

    @Query("select e from MyEntity e where e.name = :name")
    List<MyEntity> findByName(String name);
}
