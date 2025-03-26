package io.quarkus.it.hibernate.jpamodelgen.data;

import java.util.List;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

@Repository
public interface MyRepository extends CrudRepository<MyEntity, Integer> {

    @Query("select e from MyEntity e where e.name like :name")
    List<MyEntity> findByName(String name);

    @Delete
    void delete(String name);

}
