
package io.quarkus.gradle.tasks;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.gradle.api.tasks.TaskAction;

public abstract class ImagePush extends ImageTask {

    @Inject
    public ImagePush(QuarkusBuildConfiguration buildConfiguration) {
        super(buildConfiguration, "Perfom an image push");
    }

    @Override
    public Map<String, String> forcedProperties() {
        return Map.of(QUARKUS_CONTAINER_IMAGE_PUSH, "true");
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
            getProject().getLogger().warn("Task: {} requires a container image extension.", getName());
            getProject().getLogger().warn("Available container image exntesions: [{}]",
                    extensions.stream().collect(Collectors.joining(", ")));
            getProject().getLogger().warn("To add an extension to the project, you can run one of the commands below:");
            extensions.forEach(e -> {
                getProject().getLogger().warn("\tgradle addExtension --extensions={}", e);
            });
        }
    }
}
