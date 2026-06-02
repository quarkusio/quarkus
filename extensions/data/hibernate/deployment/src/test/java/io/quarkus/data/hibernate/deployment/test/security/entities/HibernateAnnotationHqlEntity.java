package io.quarkus.data.hibernate.deployment.test.security.entities;

import java.util.List;

import jakarta.persistence.Entity;

import org.hibernate.annotations.processing.HQL;

import io.quarkus.data.hibernate.ManagedEntity;
import io.quarkus.data.hibernate.ManagedRepository;
import io.quarkus.security.Authenticated;

@Entity
public class HibernateAnnotationHqlEntity extends ManagedEntity {

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

    public interface InnerPanacheRepository extends ManagedRepository<HibernateAnnotationHqlEntity> {
    }
}
