package io.quarkus.rest.server.test.simple;

public class MyParameter {
    private String value;

    public MyParameter(String str) {
        this.value = "WRONG CONSTRUCTOR";
    }

    public MyParameter(String str, String str2) {
        this.value = str + str2;
    }

    @Override
    public String toString() {
        return value;
    }
}
