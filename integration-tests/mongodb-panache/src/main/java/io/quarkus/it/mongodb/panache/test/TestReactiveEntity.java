package io.quarkus.it.mongodb.panache.test;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;

public class TestReactiveEntity extends ReactivePanacheMongoEntity {
    public String title;
    public String category;
    public String description;
    public int cpt = 1;

    public TestReactiveEntity() {
    }

    public TestReactiveEntity(String title, String category, String description) {
        this.title = title;
        this.category = category;
        this.description = description;
    }
}
