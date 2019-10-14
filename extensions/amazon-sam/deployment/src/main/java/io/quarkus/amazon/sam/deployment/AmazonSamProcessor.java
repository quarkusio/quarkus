package io.quarkus.amazon.sam.deployment;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.quarkus.amazon.sam.AmazonSamConfig;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.vertx.http.deployment.RequireVirtualHttpBuildItem;

public class AmazonSamProcessor {

    public static final String SAM_HANDLER = "io.quarkus.amazon.lambda.resteasy.runtime.container.StreamLambdaHandler::handleRequest";
    public static final String SAM_RUNTIME = "java8";

    AmazonSamConfig config;

    @BuildStep
    public RequireVirtualHttpBuildItem requestVirtualHttp(LaunchModeBuildItem launchMode) {
        return launchMode.getLaunchMode() == LaunchMode.NORMAL ? RequireVirtualHttpBuildItem.MARKER : null;
    }

    @SuppressWarnings("unchecked")
    @BuildStep
    void sam() {
        if (config.updateConfig) {
            try {
                File configFile = new File(config.template);
                if (!configFile.exists()) {
                    try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("template.yaml")) {
                        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                        Map template = mapper.readValue(inputStream, LinkedHashMap.class);
                        Map<String, Object> resources = walk(template, "Resources");

                        Map<String, Object> resource = walk(resources, "QuarkusSam");
                        resources.put(config.resourceName, resource);
                        resources.remove("QuarkusSam");

                        mapper.writer().withDefaultPrettyPrinter().writeValue(configFile, template);
                    }
                } else {
                    try (InputStream inputStream = new FileInputStream(configFile)) {
                        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                        Map template = mapper.readValue(inputStream, LinkedHashMap.class);
                        Map<String, Object> resources = walk(template, "Resources");

                        Map<String, Object> resource = walk(resources, config.resourceName);

                        if (resource == null && resources.size() == 1) {
                            resource = (Map<String, Object>) resources.entrySet().iterator().next().getValue();
                        }
                        if (resource != null) {
                            Map<String, Object> properties = walk(resource, "Properties");
                            properties.put("Handler", SAM_HANDLER);
                            properties.put("Runtime", SAM_RUNTIME);
                        }

                        mapper.writer().withDefaultPrettyPrinter().writeValue(configFile, template);
                    }

                }
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T walk(final Map template, final String... keys) {
        Map map = template;
        for (int i = 0; i < keys.length - 1; i++) {
            map = (Map) map.get(keys[i]);
            if (map == null) {
                throw new IllegalArgumentException("No object found with the key: " + keys[i]);
            }
        }
        return (T) map.get(keys[keys.length - 1]);
    }

}
