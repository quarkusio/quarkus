package io.quarkus.resteasy.reactive.links.deployment;

public class TestRecord extends AbstractEntity {

    private String value;

    public TestRecord() {
    }

    public TestRecord(int id, String slug, String value) {
        super(id, slug);
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
