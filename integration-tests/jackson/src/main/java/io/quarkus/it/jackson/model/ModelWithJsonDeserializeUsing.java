package io.quarkus.it.jackson.model;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = ModelWithJsonDeserializeUsing.CustomDeserializer.class)
public class ModelWithJsonDeserializeUsing {

    private String someValue;

    public ModelWithJsonDeserializeUsing() {
    }

    public ModelWithJsonDeserializeUsing(String someValue) {
        this.someValue = someValue;
    }

    public String getSomeValue() {
        return someValue;
    }

    public static class CustomDeserializer extends ValueDeserializer<ModelWithJsonDeserializeUsing> {

        @Override
        public ModelWithJsonDeserializeUsing deserialize(JsonParser p, DeserializationContext ctxt) {
            return new ModelWithJsonDeserializeUsing("constant");
        }
    }
}
