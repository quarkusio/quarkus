package io.quarkus.data.hibernate.deployment.test.security.entities;

import java.util.List;

import jakarta.persistence.Entity;

import org.hibernate.annotations.processing.SQL;

import io.quarkus.data.hibernate.PanacheEntity;
import io.quarkus.data.hibernate.PanacheRepository;
import io.quarkus.security.Authenticated;

@Entity
public class HibernateAnnotationSqlEntity extends PanacheEntity {

    public String name;

    public interface MethodSecured {
        @Authenticated
        @SQL("select * from HibernateAnnotationSqlEntity where name = ?1")
        List<HibernateAnnotationSqlEntity> findByName(String name);
    }

    @Authenticated
    public interface ClassSecured {
        @SQL("select * from HibernateAnnotationSqlEntity where name = ?1")
        List<HibernateAnnotationSqlEntity> findByName(String name);
    }

    public interface InnerPanacheRepository extends PanacheRepository<HibernateAnnotationSqlEntity> {
    }
}
