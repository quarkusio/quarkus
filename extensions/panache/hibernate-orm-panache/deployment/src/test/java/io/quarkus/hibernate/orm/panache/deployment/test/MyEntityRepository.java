package io.quarkus.hibernate.orm.panache.deployment.test;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.orm.panache.PanacheRepository;

@ApplicationScoped
public class MyEntityRepository implements PanacheRepository<MyEntity> {
}
