package io.quarkus.it.hibernate.processor.data.puother;

import java.util.List;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Query;

import io.quarkus.security.Authenticated;

public interface ParentMethodSecurityParentMyOtherRepository extends CrudRepository<MyOtherEntity, Integer> {

    @Authenticated
    @Query("select e from MyOtherEntity e where e.name like :name")
    List<MyOtherEntity> findByName(String name);

}
