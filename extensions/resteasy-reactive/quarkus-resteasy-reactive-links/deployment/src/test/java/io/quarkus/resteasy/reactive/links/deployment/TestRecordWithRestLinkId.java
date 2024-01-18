package io.quarkus.resteasy.reactive.links.deployment;

import io.quarkus.resteasy.reactive.links.RestLinkId;

public class TestRecordWithRestLinkId {

    @RestLinkId
    private int restLinkId;
    private String name;

    public TestRecordWithRestLinkId() {
    }

    public TestRecordWithRestLinkId(int restLinkId, String value) {
        this.restLinkId = restLinkId;
        this.name = value;
    }

    public int getRestLinkId() {
        return restLinkId;
    }

    public void setRestLinkId(int restLinkId) {
        this.restLinkId = restLinkId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
