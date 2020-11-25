package io.quarkus.rest.server.test.simple;

public class ParameterWithFromString {

    private String val;

    public ParameterWithFromString(String val) {
        this.val = val;
    }

    public static ParameterWithFromString fromString(String val) {
        return new ParameterWithFromString(val);
    }

    @Override
    public String toString() {
        return "ParameterWithFromString[val=" + val + "]";
    }
}
