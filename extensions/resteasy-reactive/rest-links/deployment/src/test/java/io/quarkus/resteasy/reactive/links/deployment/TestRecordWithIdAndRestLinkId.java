package io.quarkus.resteasy.reactive.links.deployment;

import io.quarkus.resteasy.reactive.links.RestLinkId;

public class TestRecordWithIdAndRestLinkId {

    @RestLinkId
    private int restLinkId;
    private int id;
    private String name;

    public TestRecordWithIdAndRestLinkId() {
    }

    public TestRecordWithIdAndRestLinkId(int restLinkId, int id, String value) {
        this.restLinkId = restLinkId;
        this.id = id;
        this.name = value;
    }

    public int getRestLinkId() {
        return restLinkId;
    }

    public void setRestLinkId(int restLinkId) {
        this.restLinkId = restLinkId;
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
