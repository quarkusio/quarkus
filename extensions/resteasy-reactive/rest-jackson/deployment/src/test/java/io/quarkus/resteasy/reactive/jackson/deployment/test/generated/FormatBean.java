package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import com.fasterxml.jackson.annotation.JsonFormat;

public class FormatBean {

    private String name;

    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private FormatShape shape;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FormatShape getShape() {
        return shape;
    }

    public void setShape(FormatShape shape) {
        this.shape = shape;
    }
}
