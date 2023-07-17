package io.quarkus.it.smallrye.config;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "implicit.converters")
public interface ImplicitConverters {
    @WithDefault("value")
    Optional<ImplicitOptional> optional();

    @WithDefault("value")
    List<ImplicitElement> list();

    Map<String, ImplicitValue> map();

    class ImplicitOptional {
        private final String value;

        public ImplicitOptional(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static ImplicitOptional of(String value) {
            return new ImplicitOptional("converted");
        }
    }

    class ImplicitElement {
        private final String value;

        public ImplicitElement(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static ImplicitElement of(String value) {
            return new ImplicitElement("converted");
        }
    }

    class ImplicitValue {
        private final String value;

        public ImplicitValue(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static ImplicitValue of(String value) {
            return new ImplicitValue("converted");
        }
    }
}
