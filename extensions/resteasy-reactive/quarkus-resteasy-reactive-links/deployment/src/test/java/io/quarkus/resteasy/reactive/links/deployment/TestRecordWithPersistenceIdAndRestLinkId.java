package io.quarkus.resteasy.reactive.links.deployment;

import io.quarkus.resteasy.reactive.links.RestLinkId;
import io.quarkus.resteasy.reactive.links.deployment.persistence.Id;

public class TestRecordWithPersistenceIdAndRestLinkId {

    @RestLinkId
    private int restLinkId;
    @Id
    private int persistenceId;
    private String name;

    public TestRecordWithPersistenceIdAndRestLinkId() {
    }

    public TestRecordWithPersistenceIdAndRestLinkId(int restLinkId, int persistenceId, String name) {
        this.restLinkId = restLinkId;
        this.persistenceId = persistenceId;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
