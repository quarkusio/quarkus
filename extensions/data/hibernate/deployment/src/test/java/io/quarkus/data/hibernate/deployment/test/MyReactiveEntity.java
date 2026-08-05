package io.quarkus.data.hibernate.deployment.test;

import java.util.List;

import jakarta.persistence.Entity;

import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;

import io.quarkus.data.hibernate.ManagedEntity;
import io.quarkus.data.hibernate.ManagedRepository;
import io.smallrye.mutiny.Uni;

@Entity
public class MyReactiveEntity extends ManagedEntity.Reactive {

    public String foo;
    public String bar;

    interface ManagedReactiveQueries extends ManagedRepository.Reactive<MyReactiveEntity> {
        default Uni<List<MyReactiveEntity>> findFoos(String val) {
            return list("foo", val);
        }

        @HQL("where foo = :val")
        Uni<List<MyReactiveEntity>> findFoosHQL(String val);

        @Find
        Uni<List<MyReactiveEntity>> findFoosFind(String foo);
    }
}
