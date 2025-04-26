package org.acme.lib;

public class StringTypeProtectedConstructor {

    private String value;

    protected StringTypeProtectedConstructor(final String value) {
        this.value = value;
    }


    public String getValue() {
        return value;
    }


    public static StringTypeProtectedConstructor fromString(final String s) {
        return new StringTypeProtectedConstructor(s);
    }


    @Override
    public String toString() {
        return value;
    }
}
