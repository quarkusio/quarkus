package io.quarkus.resteasy.reactive.jackson.deployment.test;

import com.fasterxml.jackson.annotation.JsonValue;

public class ItemJsonValueInheritedFromInterface {

    public interface HasValue<T> {
        @JsonValue
        T getValue();
    }

    public static class Wrapper implements HasValue<String> {
        private final String value;

        public Wrapper(String value) {
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }
    }
}
