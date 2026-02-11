package io.quarkus.hibernate.panache.deployment.test.processor;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

public class EntityContainer {
    @Entity
    public static class MyEntity {
        @Id
        String id;
    }

    @Entity
    public static class MyEntity_ {
        @Id
        String id;
    }
}
