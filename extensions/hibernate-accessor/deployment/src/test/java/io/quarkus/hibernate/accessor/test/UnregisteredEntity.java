package io.quarkus.hibernate.accessor.test;

public class UnregisteredEntity {

    private String value;

    public UnregisteredEntity() {
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
