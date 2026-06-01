package io.quarkus.data.hibernate.deployment.test;

import java.util.List;

import jakarta.persistence.Entity;

import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;

import io.quarkus.data.hibernate.ManagedEntity;
import io.quarkus.data.hibernate.ManagedRepository;

@Entity
public class MyEntity extends ManagedEntity {
    public String foo;
    public String bar;

    interface ManagedBlockingQueries extends ManagedRepository<MyEntity> {
        default List<MyEntity> findFoos(String val) {
            return list("foo", val);
        }

        @HQL("where foo = :val")
        List<MyEntity> findFoosHQL(String val);

        @Find
        List<MyEntity> findFoosFind(String foo);
    }

    interface FindOnlyRepo {
        @Find
        List<MyEntity> findByFoo(String foo);
    }
}
