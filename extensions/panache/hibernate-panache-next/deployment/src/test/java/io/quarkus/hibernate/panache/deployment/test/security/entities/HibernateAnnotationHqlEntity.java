package io.quarkus.hibernate.panache.deployment.test.security.entities;

import java.util.List;

import jakarta.persistence.Entity;

import org.hibernate.annotations.processing.HQL;

import io.quarkus.hibernate.panache.PanacheEntity;
import io.quarkus.hibernate.panache.PanacheRepository;
import io.quarkus.security.Authenticated;

@Entity
public class HibernateAnnotationHqlEntity extends PanacheEntity {

    public String name;

    public interface MethodSecured {
        @Authenticated
        @HQL("where name = :name")
        List<HibernateAnnotationHqlEntity> findByName(String name);
    }

    @Authenticated
    public interface ClassSecured {
        @HQL("where name = :name")
        List<HibernateAnnotationHqlEntity> findByName(String name);
    }

    public interface InnerPanacheRepository extends PanacheRepository<HibernateAnnotationHqlEntity> {
    }
}
