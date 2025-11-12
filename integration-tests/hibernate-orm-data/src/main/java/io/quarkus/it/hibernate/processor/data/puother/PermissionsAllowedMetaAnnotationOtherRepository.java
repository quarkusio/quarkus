package io.quarkus.it.hibernate.processor.data.puother;

import java.util.List;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

import io.quarkus.it.hibernate.processor.data.security.CanFindByName;

@CanFindByName
@Repository(dataStore = "other")
public interface PermissionsAllowedMetaAnnotationOtherRepository extends CrudRepository<MyOtherEntity, Integer> {

    @Query("select e from MyOtherEntity e where e.name like :name")
    List<MyOtherEntity> findByName(String name);

}
