package io.quarkus.hibernate.panache.deployment.test;

import io.quarkus.hibernate.panache.PanacheRepository;

// Test for https://github.com/quarkusio/quarkus/issues/52975
public class MyEntityRepository implements PanacheRepository<MyEntity> {
}
