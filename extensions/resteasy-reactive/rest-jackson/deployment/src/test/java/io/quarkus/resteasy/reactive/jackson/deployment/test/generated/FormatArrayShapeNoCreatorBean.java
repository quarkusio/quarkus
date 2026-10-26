package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

// no all-args constructor, otherwise Jackson would use it as an implicit creator instead of the setters
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder({ "name", "value" })
public class FormatArrayShapeNoCreatorBean {

    private String name;
    private int value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
