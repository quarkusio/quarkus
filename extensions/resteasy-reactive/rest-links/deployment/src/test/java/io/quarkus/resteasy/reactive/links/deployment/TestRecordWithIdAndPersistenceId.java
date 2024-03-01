package io.quarkus.resteasy.reactive.links.deployment;

import io.quarkus.resteasy.reactive.links.deployment.persistence.Id;

public class TestRecordWithIdAndPersistenceId {

    @Id
    private int persistenceId;
    private int id;
    private String name;

    public TestRecordWithIdAndPersistenceId() {
    }

    public TestRecordWithIdAndPersistenceId(int persistenceId, int id, String value) {
        this.persistenceId = persistenceId;
        this.id = id;
        this.name = value;
    }

    public int getPersistenceId() {
        return persistenceId;
    }

    public void setPersistenceId(int persistenceId) {
        this.persistenceId = persistenceId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
