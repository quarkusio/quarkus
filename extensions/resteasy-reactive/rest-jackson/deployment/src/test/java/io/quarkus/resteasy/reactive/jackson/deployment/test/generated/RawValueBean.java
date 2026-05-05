package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import com.fasterxml.jackson.annotation.JsonRawValue;

public class RawValueBean {

    private String name;

    @JsonRawValue
    private String rawJson;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRawJson() {
        return rawJson;
    }

    public void setRawJson(String rawJson) {
        this.rawJson = rawJson;
    }
}
