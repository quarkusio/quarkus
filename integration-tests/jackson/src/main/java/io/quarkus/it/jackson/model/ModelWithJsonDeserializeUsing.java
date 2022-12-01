package io.quarkus.it.jackson.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

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

    public static class CustomDeserializer extends JsonDeserializer<ModelWithJsonDeserializeUsing> {

        @Override
        public ModelWithJsonDeserializeUsing deserialize(JsonParser p, DeserializationContext ctxt) {
            return new ModelWithJsonDeserializeUsing("constant");
        }
    }
}
