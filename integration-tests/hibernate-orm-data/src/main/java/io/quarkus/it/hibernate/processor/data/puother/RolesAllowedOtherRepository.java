package io.quarkus.it.hibernate.processor.data.puother;

import java.util.List;

import jakarta.annotation.security.RolesAllowed;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

@RolesAllowed("admin")
@Repository(dataStore = "other")
public interface RolesAllowedOtherRepository extends CrudRepository<MyOtherEntity, Integer> {

    @Query("select e from MyOtherEntity e where e.name like :name")
    List<MyOtherEntity> findByName(String name);

}
