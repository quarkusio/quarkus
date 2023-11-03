package io.quarkus.qute.generator;

import io.quarkus.qute.TemplateData;

@TemplateData(namespace = "MyEnum")
public enum MyEnum {

    ONE,
    BAR,
    CHARLIE;

    MyEnum() {
    }

    public String getName() {
        return this.toString().toLowerCase();
    }

}
