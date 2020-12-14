package io.quarkus.it.jpa.attributeconverter;

public final class MyDataNotRequiringCDI {
    private final String content;

    public MyDataNotRequiringCDI(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }
}
