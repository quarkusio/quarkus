package io.quarkus.it.jpa.attributeconverter;

public final class MyDataRequiringCDI {
    private final String content;

    public MyDataRequiringCDI(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }
}
