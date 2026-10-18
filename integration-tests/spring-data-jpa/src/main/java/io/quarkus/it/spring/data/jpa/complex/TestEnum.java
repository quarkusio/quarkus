package io.quarkus.it.spring.data.jpa.complex;

public enum TestEnum {
    TEST("Test"),
    TEST2("Test2"),;

    TestEnum(String value) {
        this.value = value;
    }

    private final String value;

    public String getValue() {
        return value;
    }
}
