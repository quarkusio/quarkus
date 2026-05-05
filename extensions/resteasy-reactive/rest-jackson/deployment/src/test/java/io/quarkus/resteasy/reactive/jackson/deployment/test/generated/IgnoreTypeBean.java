package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "name", "metadata" })
public class IgnoreTypeBean {

    private String name;
    private IgnoredType metadata;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public IgnoredType getMetadata() {
        return metadata;
    }

    public void setMetadata(IgnoredType metadata) {
        this.metadata = metadata;
    }
}
