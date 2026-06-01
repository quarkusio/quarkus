package io.quarkus.data.hibernate.deployment.test.security.entities;

import java.util.List;

import jakarta.data.Order;
import jakarta.persistence.Entity;

import org.hibernate.annotations.processing.Find;

import io.quarkus.data.hibernate.ManagedEntity;
import io.quarkus.data.hibernate.ManagedRepository;
import io.quarkus.security.Authenticated;

@Entity
public class GenericRepoEntity extends ManagedEntity {

    public String name;

    public interface ParentRepo<T extends GenericRepoEntity> {
        @Find
        List<T> findAll(Order<T> order);
    }

    public interface ChildRepo extends ParentRepo<GenericRepoEntity> {
        @Authenticated
        @Find
        List<GenericRepoEntity> securedFindAll(Order<GenericRepoEntity> order);
    }

    public interface InnerPanacheRepository extends ManagedRepository<GenericRepoEntity> {
    }
}
