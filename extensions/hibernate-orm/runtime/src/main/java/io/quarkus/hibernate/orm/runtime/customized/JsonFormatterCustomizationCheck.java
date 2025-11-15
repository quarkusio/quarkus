package io.quarkus.hibernate.orm.runtime.customized;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import jakarta.enterprise.inject.Instance;
import jakarta.json.bind.Jsonb;

import org.eclipse.microprofile.config.ConfigProvider;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.jackson.ObjectMapperCustomizer;
import io.quarkus.jsonb.JsonbConfigCustomizer;

/**
 * Test whether the underlying Jackson Object Mapper / JSON-B used to create the "built-in" format mapper
 * were modified and cannot be safely replaced by the Hibernate ORM's default ones.
 */
public interface JsonFormatterCustomizationCheck extends Function<ArcContainer, List<String>> {
    @Override
    List<String> apply(ArcContainer container);

    static JsonFormatterCustomizationCheck jsonFormatterCustomizationCheckSupplier(boolean required, boolean isJackson) {
        if (!required) {
            return new NotModifiedJsonFormatterCustomizationCheck();
        }

        if (isJackson) {
            return new JacksonJsonFormatterCustomizationCheck();
        }
        return new JsonbJsonFormatterCustomizationCheck();
    }

    class NotModifiedJsonFormatterCustomizationCheck implements JsonFormatterCustomizationCheck {
        @Override
        public List<String> apply(ArcContainer container) {
            return List.of();
        }
    }

    class JacksonJsonFormatterCustomizationCheck implements JsonFormatterCustomizationCheck {
        @Override
        public List<String> apply(ArcContainer container) {
            InstanceHandle<ObjectMapper> objectMapperInstance = container.instance(ObjectMapper.class);
            if (!objectMapperInstance.isAvailable()) {
                // We have no mapper so it wasn't modified, and we've probably used the JSONB instead.
                return List.of();
            }
            List<String> causes = new ArrayList<>();
            if (!objectMapperInstance.getBean().isDefaultBean()) {
                // A bean producer was used and the ObjectMapper is a user custom bean...
                causes.add(
                        "ObjectMapper instance is not the Quarkus default one. A bean producer was likely used to replace it.");
            }

            Instance<ObjectMapperCustomizer> customizers = container.select(ObjectMapperCustomizer.class);
            if (!customizers.isUnsatisfied()) {
                // There most likely are the following customizer available:
                //  - io.quarkus.jackson.customizer.RegisterSerializersAndDeserializersCustomizer  --- this one ... Do we need to check if any serializers were added?
                //  - io.quarkus.jackson.runtime.ConfigurationCustomizer  --- applies the "config properties", we check for those later hence "safe to ignore"
                //  - io.quarkus.jackson.runtime.VertxHybridPoolObjectMapperCustomizer  --- doesn't look like this one affects the serialization -- ignore it
                Set<String> allowedCustomizers = Set.of(
                        "io.quarkus.jackson.customizer.RegisterSerializersAndDeserializersCustomizer",
                        "io.quarkus.jackson.runtime.ConfigurationCustomizer",
                        "io.quarkus.jackson.runtime.VertxHybridPoolObjectMapperCustomizer");
                for (Instance.Handle<ObjectMapperCustomizer> handle : customizers.handles()) {
                    if (allowedCustomizers.contains(handle.getBean().getBeanClass().getName())) {
                        continue;
                    }
                    // ObjectMapper was potentially customized
                    causes.add("Detected '" + handle.getBean().getBeanClass().getName() + "' bean registered. "
                            + "It may have customized the ObjectMapper in a way not compatible with the default one. "
                            + "Review the customizer and apply customizations you need for the database serialization/deserialization of JSON fields to your custom mapper.");
                }
            }

            // these won't affect representation of the objects in the DB so it's probably somewhat "safe" to allow them:
            Set<String> acceptableConfigs = Set.of("quarkus.jackson.fail-on-unknown-properties",
                    "quarkus.jackson.fail-on-empty-beans");
            Map<String, String> expectedDefaults = Map.of(
                    "quarkus.jackson.write-dates-as-timestamps", "true",
                    "quarkus.jackson.write-durations-as-timestamps", "true",
                    "quarkus.jackson.accept-case-insensitive-enums", "false",
                    "quarkus.jackson.timezone", "UTC");
            Map<String, String> actionsToTake = Map.of(
                    "quarkus.jackson.write-dates-as-timestamps",
                    "disable the corresponding serialization feature, i.e. 'mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)'",
                    "quarkus.jackson.write-durations-as-timestamps",
                    "disable the corresponding serialization feature, i.e. 'mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)'",
                    "quarkus.jackson.accept-case-insensitive-enums",
                    "enable the corresponding mapper feature, i.e. 'mapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)'",
                    "quarkus.jackson.timezone", "configure the mapper timezone, i.e. 'mapper.setTimeZone(yourTimezone)'");
            for (String propertyName : ConfigProvider.getConfig().getPropertyNames()) {
                if (propertyName.startsWith("quarkus.jackson.") && !acceptableConfigs.contains(propertyName)) {
                    String okValue = expectedDefaults.get(propertyName);
                    if (okValue != null && !okValue
                            .equalsIgnoreCase(ConfigProvider.getConfig().getConfigValue(propertyName).getRawValue())) {
                        causes.add("Detected '" + propertyName + "' property set to '"
                                + ConfigProvider.getConfig().getConfigValue(propertyName).getRawValue() + "'. "
                                + "To make your custom ObjectMapper compatible with this configuration: "
                                + actionsToTake.get(propertyName) + ".");
                    }
                }
            }

            return causes;
        }
    }

    class JsonbJsonFormatterCustomizationCheck implements JsonFormatterCustomizationCheck {
        @Override
        public List<String> apply(ArcContainer container) {
            InstanceHandle<Jsonb> jsonbInstance = container.instance(Jsonb.class);
            if (!jsonbInstance.isAvailable()) {
                // We have no JSON-B bean so it wasn't modified.
                return List.of();
            }
            List<String> causes = new ArrayList<>();
            if (!jsonbInstance.getBean().isDefaultBean()) {
                // A bean producer was used and the JSON-B is a user custom bean...
                causes.add("Jsonb instance is not the Quarkus default one. A bean producer was likely used to replace it.");
            }

            Instance<JsonbConfigCustomizer> customizers = container.select(JsonbConfigCustomizer.class);
            // There most likely is the following customizer available:
            //  - io.quarkus.jsonb.customizer.RegisterSerializersAndDeserializersCustomizer  --- this one ... Do we need to check if any serializers were added?
            Set<String> allowedCustomizers = Set.of(
                    "io.quarkus.jsonb.customizer.RegisterSerializersAndDeserializersCustomizer");
            for (Instance.Handle<JsonbConfigCustomizer> handle : customizers.handles()) {
                if (allowedCustomizers.contains(handle.getBean().getBeanClass().getName())) {
                    continue;
                }

                // JSON-B was potentially customized
                causes.add("Detected '" + handle.getBean().getBeanClass().getName() + "' bean registered. "
                        + "It may have customized the Jsonb in a way not compatible with the default one. "
                        + "Review the customizer and apply customizations you need for the database serialization/deserialization of JSON fields to your custom Jsonb.");
            }

            // JSON-B does not have the config properties so nothing else to check..
            return causes;
        }
    }
}
