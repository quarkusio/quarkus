package io.quarkus.it.hibernate.processor.data.puother;

import java.util.List;
import java.util.stream.Stream;

import jakarta.data.Order;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

@Repository(dataStore = "other")
public interface MyOtherRepository extends CrudRepository<MyOtherEntity, Integer> {

    @Find
    Stream<MyOtherEntity> findAll(Order<MyOtherEntity> order);

    @Query("select e from MyOtherEntity e where e.name like :name")
    List<MyOtherEntity> findByName(String name);

    @Delete
    void delete(String name);

}
