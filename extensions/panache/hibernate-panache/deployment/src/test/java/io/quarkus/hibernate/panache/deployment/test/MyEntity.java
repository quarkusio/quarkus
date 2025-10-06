package io.quarkus.hibernate.panache.deployment.test;

import java.util.List;

import jakarta.persistence.Entity;

import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;

import io.quarkus.hibernate.panache.PanacheEntity;
import io.quarkus.hibernate.panache.PanacheRepository;

@Entity
public class MyEntity extends PanacheEntity {
    public String foo;
    public String bar;

    interface ManagedBlockingQueries extends PanacheRepository<MyEntity> {
        default List<MyEntity> findFoos(String val) {
            return list("foo", val);
        }

        @HQL("where foo = :val")
        List<MyEntity> findFoosHQL(String val);

        @Find
        List<MyEntity> findFoosFind(String foo);
    }
}
