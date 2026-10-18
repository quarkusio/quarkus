package io.quarkus.data.hibernate.deployment.test.security.entities;

import java.util.List;

import jakarta.persistence.Entity;

import org.hibernate.annotations.processing.Find;

import io.quarkus.data.hibernate.ManagedEntity;
import io.quarkus.data.hibernate.ManagedRepository;
import io.quarkus.security.Authenticated;

@Entity
public class HibernateAnnotationFindEntity extends ManagedEntity {

    public String name;

    public interface MethodSecured {
        @Authenticated
        @Find
        List<HibernateAnnotationFindEntity> findByName(String name);
    }

    @Authenticated
    public interface ClassSecured {
        @Find
        List<HibernateAnnotationFindEntity> findByName(String name);
    }

    public interface InnerPanacheRepository extends ManagedRepository<HibernateAnnotationFindEntity> {
    }
}
