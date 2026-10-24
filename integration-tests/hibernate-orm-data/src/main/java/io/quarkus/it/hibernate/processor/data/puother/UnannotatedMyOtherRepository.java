package io.quarkus.it.hibernate.processor.data.puother;

import java.util.List;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Query;

// Unannotated repository (no @Repository): its persistence unit must be resolved from the package-based rules,
// which assign the "puother" package to the "other" persistence unit.
public interface UnannotatedMyOtherRepository extends CrudRepository<MyOtherEntity, Integer> {

    @Query("select e from MyOtherEntity e where e.name = :name")
    List<MyOtherEntity> findByName(String name);
}
