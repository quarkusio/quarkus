package io.quarkus.resteasy.reactive.links.deployment;

import io.quarkus.resteasy.reactive.links.deployment.persistence.Id;

public class TestRecordWithPersistenceId {

    @Id
    private int persistenceId;
    private String name;

    public TestRecordWithPersistenceId() {
    }

    public TestRecordWithPersistenceId(int persistenceId, String value) {
        this.persistenceId = persistenceId;
        this.name = value;
    }

    public int getPersistenceId() {
        return persistenceId;
    }

    public void setPersistenceId(int persistenceId) {
        this.persistenceId = persistenceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
