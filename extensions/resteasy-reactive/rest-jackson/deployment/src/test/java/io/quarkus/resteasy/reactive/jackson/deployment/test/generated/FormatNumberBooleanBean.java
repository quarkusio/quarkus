package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import com.fasterxml.jackson.annotation.JsonFormat;

public class FormatNumberBooleanBean {

    private String name;

    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private boolean enabled;

    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Boolean optional;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getOptional() {
        return optional;
    }

    public void setOptional(Boolean optional) {
        this.optional = optional;
    }
}
