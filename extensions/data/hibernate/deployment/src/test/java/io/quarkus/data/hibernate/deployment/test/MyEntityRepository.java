package io.quarkus.data.hibernate.deployment.test;

import io.quarkus.data.hibernate.ManagedRepository;

// Test for https://github.com/quarkusio/quarkus/issues/52975
public class MyEntityRepository implements ManagedRepository<MyEntity> {
}
