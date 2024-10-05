package io.quarkus.devui.runtime.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Properties;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.devui.runtime.config.ApplicationPropertiesService.ApplicationPropertiesNotFoundException;
import io.quarkus.devui.runtime.config.ApplicationPropertiesService.ResourceDirectoryNotFoundException;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class ConfigJsonRPCService {

    @Inject
    ConfigDescriptionBean configDescriptionBean;
    @Inject
    ApplicationPropertiesService applicationPropertiesService;

    public record ApplicationConfigDescriptionDto(
            ConfigDescription configDescription,
            String sourceValue, // value in application.properties, if available
            String profile) {

    }

    private static ApplicationConfigDescriptionDto createApplicationConfigDescription(
            Properties properties,
            ConfigDescription config) {
        // TODO only application.properties are read (no profile-specific properties)
        // first profile wins
        final var profiles = ConfigUtils.getProfiles();
        for (String profile : profiles) {
            final var rawValue = properties.getProperty("%" + profile + "." + config.getName());
            if (null != rawValue) {
                return new ApplicationConfigDescriptionDto(
                        config,
                        rawValue,
                        profile);
            }
        }
        return new ApplicationConfigDescriptionDto(
                config,
                properties.getProperty(config.getName()),
                null);
    }

    @SuppressWarnings("unused") // used in Configuration Form Editor
    public JsonArray getFullPropertyConfiguration() throws IOException {
        final Properties properties = new Properties();
        try {
            properties.putAll(applicationPropertiesService.readApplicationProperties());
        } catch (ResourceDirectoryNotFoundException | ApplicationPropertiesNotFoundException ex) {
            // don't do anything, properties will be just empty
        }
        return new JsonArray(
                configDescriptionBean
                        .getAllConfig()
                        .stream()
                        .map(config -> createApplicationConfigDescription(properties, config))
                        .toList());
    }

    record PropertyDto(
            String key,
            String value) {
    }

    public JsonArray getProjectProperties() throws IOException {
        return new JsonArray(
                applicationPropertiesService
                        .readApplicationProperties()
                        .entrySet()
                        .stream()
                        .map(entry -> new PropertyDto(
                                String.valueOf(entry.getKey()),
                                String.valueOf(entry.getValue())))
                        .toList());
    }

    @SuppressWarnings("unused") // used in Configuration Source Editor
    public JsonObject getProjectPropertiesAsString() throws IOException {
        JsonObject response = new JsonObject();
        try (Reader reader = applicationPropertiesService.createApplicationPropertiesReader();
                BufferedReader bufferedReader = new BufferedReader(reader)) {
            final var content = bufferedReader
                    .lines()
                    .collect(Collectors.joining("\n"));
            response.put("type", "properties");
            response.put("value", content);
        } catch (ResourceDirectoryNotFoundException ex) {
            response.put("error", "Unable to manage configurations - no resource directory found");
        } catch (ApplicationPropertiesNotFoundException ex) {
            response.put("type", "properties");
            response.put("value", "");
        }
        return response;
    }
}
