package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE)
public class AutoDetectBean {

    private String visibleField;
    private int count;

    public AutoDetectBean() {
    }

    public AutoDetectBean(String visibleField, int count) {
        this.visibleField = visibleField;
        this.count = count;
    }
}
