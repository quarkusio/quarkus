package io.quarkus.it.hibernate.jpamodelgen.data;

import java.util.List;
import java.util.stream.Stream;

import jakarta.data.Order;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

@Repository
public interface MyRepository extends CrudRepository<MyEntity, Integer> {

    @Find
    Stream<MyEntity> findAll(Order<MyEntity> order);

    @Query("select e from MyEntity e where e.name like :name")
    List<MyEntity> findByName(String name);

    @Delete
    void delete(String name);

}
