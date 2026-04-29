package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class IncludeBean {

    private String name;
    private String nullableField;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String emptyField;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNullableField() {
        return nullableField;
    }

    public void setNullableField(String nullableField) {
        this.nullableField = nullableField;
    }

    public String getEmptyField() {
        return emptyField;
    }

    public void setEmptyField(String emptyField) {
        this.emptyField = emptyField;
    }
}
