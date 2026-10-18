package io.quarkus.resteasy.reactive.jackson.deployment.test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PolymorphicCreatorProperty {

    private PolymorphicItem item;

    public PolymorphicCreatorProperty() {
    }

    @JsonCreator
    public PolymorphicCreatorProperty(@JsonProperty(value = "item", required = true) PolymorphicItem item) {
        this.item = item;
    }

    public PolymorphicItem getItem() {
        return item;
    }

    public void setItem(PolymorphicItem item) {
        this.item = item;
    }
}
