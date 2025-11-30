package io.quarkus.it.hibernate.processor.data.puother;

import java.util.List;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

@DenyAll
@Repository(dataStore = "other")
public interface DenyAllMyOtherRepository extends CrudRepository<MyOtherEntity, Integer> {

    @Query("select e from MyOtherEntity e where e.name like :name")
    List<MyOtherEntity> findByName(String name);

    @PermitAll
    @Query("select e from MyOtherEntity e where e.name like :name")
    List<MyOtherEntity> findByNamePermitAll(String name);

}
