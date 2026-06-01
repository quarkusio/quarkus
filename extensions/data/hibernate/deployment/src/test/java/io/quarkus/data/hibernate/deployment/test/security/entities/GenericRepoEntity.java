package io.quarkus.data.hibernate.deployment.test.security.entities;

import java.util.List;

import jakarta.data.Order;
import jakarta.persistence.Entity;

import org.hibernate.annotations.processing.Find;

import io.quarkus.data.hibernate.PanacheEntity;
import io.quarkus.data.hibernate.PanacheRepository;
import io.quarkus.security.Authenticated;

@Entity
public class GenericRepoEntity extends PanacheEntity {

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

    public interface InnerPanacheRepository extends PanacheRepository<GenericRepoEntity> {
    }
}
