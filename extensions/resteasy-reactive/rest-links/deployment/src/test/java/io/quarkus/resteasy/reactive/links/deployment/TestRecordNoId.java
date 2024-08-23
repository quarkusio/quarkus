package io.quarkus.resteasy.reactive.links.deployment;

public class TestRecordNoId {

    private String name;

    public TestRecordNoId() {
    }

    public TestRecordNoId(String value) {
        this.name = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
