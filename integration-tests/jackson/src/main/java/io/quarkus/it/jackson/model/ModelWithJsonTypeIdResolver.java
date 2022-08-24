package io.quarkus.it.jackson.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;

@JsonTypeIdResolver(CustomTypeResolver.class)
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
public abstract class ModelWithJsonTypeIdResolver {

    public ModelWithJsonTypeIdResolver() {
    }

    @JsonIgnore
    public abstract String getType();

    public static class SubclassOne extends ModelWithJsonTypeIdResolver {
        public SubclassOne() {
        }

        @Override
        public String getType() {
            return "ONE";
        }
    }

    public static class SubclassTwo extends ModelWithJsonTypeIdResolver {
        public SubclassTwo() {
        }

        @Override
        public String getType() {
            return "TWO";
        }
    }
}
