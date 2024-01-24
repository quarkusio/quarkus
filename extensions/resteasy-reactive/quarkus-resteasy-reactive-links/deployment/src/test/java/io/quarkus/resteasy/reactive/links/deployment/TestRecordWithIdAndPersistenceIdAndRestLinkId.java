package io.quarkus.resteasy.reactive.links.deployment;

import io.quarkus.resteasy.reactive.links.RestLinkId;
import io.quarkus.resteasy.reactive.links.deployment.persistence.Id;

public class TestRecordWithIdAndPersistenceIdAndRestLinkId {

    @RestLinkId
    private int restLinkId;
    @Id
    private int persistenceId;
    private int id;
    private String name;

    public TestRecordWithIdAndPersistenceIdAndRestLinkId() {
    }

    public TestRecordWithIdAndPersistenceIdAndRestLinkId(int restLinkId, int persistenceId, int id, String name) {
        this.restLinkId = restLinkId;
        this.persistenceId = persistenceId;
        this.id = id;
        this.name = name;
    }

    public int getRestLinkId() {
        return restLinkId;
    }

    public void setRestLinkId(int restLinkId) {
        this.restLinkId = restLinkId;
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
