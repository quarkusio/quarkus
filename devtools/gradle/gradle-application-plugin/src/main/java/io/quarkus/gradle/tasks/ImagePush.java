
package io.quarkus.gradle.tasks;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.gradle.api.provider.MapProperty;
import org.gradle.api.tasks.TaskAction;

public abstract class ImagePush extends ImageTask {

    @Inject
    public ImagePush() {
        super("Perform an image push");
        MapProperty<String, String> forcedProperties = extension().forcedPropertiesProperty();
        forcedProperties.put(QUARKUS_CONTAINER_IMAGE_BUILD, "true");
        forcedProperties.put(QUARKUS_CONTAINER_IMAGE_PUSH, "true");
    }

    @TaskAction
    public void checkRequiredExtensions() {
        List<String> containerImageExtensions = getProject().getConfigurations().stream()
                .flatMap(c -> c.getDependencies().stream())
                .map(d -> d.getName())
                .filter(n -> n.startsWith(QUARKUS_CONTAINER_IMAGE_PREFIX))
                .map(n -> n.replaceAll("-deployment$", ""))
                .collect(Collectors.toList());

        List<String> extensions = Arrays.stream(ImageBuild.Builder.values()).map(b -> QUARKUS_CONTAINER_IMAGE_PREFIX + b.name())
                .collect(Collectors.toList());

        if (containerImageExtensions.isEmpty()) {
            getLogger().warn("Task: {} requires a container image extension.", getName());
            getLogger().warn("Available container image exntesions: [{}]",
                    extensions.stream().collect(Collectors.joining(", ")));
            getLogger().warn("To add an extension to the project, you can run one of the commands below:");
            extensions.forEach(e -> {
                getLogger().warn("\tgradle addExtension --extensions={}", e);
            });
        }
    }
}
