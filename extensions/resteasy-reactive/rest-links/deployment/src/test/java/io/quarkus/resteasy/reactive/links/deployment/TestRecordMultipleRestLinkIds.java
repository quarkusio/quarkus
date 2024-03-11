package io.quarkus.resteasy.reactive.links.deployment;

import io.quarkus.resteasy.reactive.links.RestLinkId;

public class TestRecordMultipleRestLinkIds {

    @RestLinkId
    private long idOne;
    @RestLinkId
    private long idTwo;

    private String name;

    public TestRecordMultipleRestLinkIds() {
    }

    public TestRecordMultipleRestLinkIds(long idOne, long idTwo, String name) {
        this.idOne = idOne;
        this.idTwo = idTwo;
        this.name = name;
    }

    public long getIdOne() {
        return idOne;
    }

    public void setIdOne(long idOne) {
        this.idOne = idOne;
    }

    public long getIdTwo() {
        return idTwo;
    }

    public void setIdTwo(long idTwo) {
        this.idTwo = idTwo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
