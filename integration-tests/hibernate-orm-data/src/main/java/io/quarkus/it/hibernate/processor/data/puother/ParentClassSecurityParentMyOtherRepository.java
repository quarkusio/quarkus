package io.quarkus.it.hibernate.processor.data.puother;

import java.util.List;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Query;

import io.quarkus.security.Authenticated;

@Authenticated
public interface ParentClassSecurityParentMyOtherRepository extends CrudRepository<MyOtherEntity, Integer> {

    @Query("select e from MyOtherEntity e where e.name like :name")
    List<MyOtherEntity> findByName(String name);

}
