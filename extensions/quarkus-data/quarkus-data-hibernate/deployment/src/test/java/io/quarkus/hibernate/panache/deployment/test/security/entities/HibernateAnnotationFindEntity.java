package io.quarkus.hibernate.panache.deployment.test.security.entities;

import java.util.List;

import jakarta.persistence.Entity;

import org.hibernate.annotations.processing.Find;

import io.quarkus.hibernate.panache.PanacheEntity;
import io.quarkus.hibernate.panache.PanacheRepository;
import io.quarkus.security.Authenticated;

@Entity
public class HibernateAnnotationFindEntity extends PanacheEntity {

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

    public interface InnerPanacheRepository extends PanacheRepository<HibernateAnnotationFindEntity> {
    }
}
