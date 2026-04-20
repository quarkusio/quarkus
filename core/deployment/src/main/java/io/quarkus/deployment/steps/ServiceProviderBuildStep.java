package io.quarkus.deployment.steps;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.GeneratedServiceProviderBuildItem;

public class ServiceProviderBuildStep {

    @BuildStep
    void generateServiceFiles(
            List<GeneratedServiceProviderBuildItem> items,
            BuildProducer<GeneratedResourceBuildItem> generatedResources) {
        if (items.isEmpty()) {
            return;
        }
        // Group implementation class names by service interface, preserving insertion order
        Map<String, StringBuilder> byInterface = new LinkedHashMap<>();
        for (GeneratedServiceProviderBuildItem item : items) {
            byInterface.computeIfAbsent(item.getServiceInterfaceName(), k -> new StringBuilder())
                    .append(item.getImplementationClassName())
                    .append(System.lineSeparator());
        }
        for (Map.Entry<String, StringBuilder> entry : byInterface.entrySet()) {
            generatedResources.produce(new GeneratedResourceBuildItem(
                    "META-INF/services/" + entry.getKey(),
                    entry.getValue().toString().getBytes(StandardCharsets.UTF_8)));
        }
        // TODO: #44657 - when targeting JPMS modules, emit module-info service entries instead of META-INF/services/ files
    }
}
