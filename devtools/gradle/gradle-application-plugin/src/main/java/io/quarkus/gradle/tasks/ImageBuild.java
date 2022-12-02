
package io.quarkus.gradle.tasks;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

public abstract class ImageBuild extends ImageTask {

    public enum Builder {
        docker,
        jib,
        buildpack,
        openshift
    }

    Builder builder = Builder.docker;

    @Option(option = "builder", description = "The container image extension to use for building the image (e.g. docker, jib, buildpack, openshift).")
    public void setBuilder(Builder builder) {
        this.builder = builder;
    }

    @Inject
    public ImageBuild(QuarkusBuildConfiguration buildConfiguration) {
        super(buildConfiguration, "Perform an image build");
    }

    @Override
    public Map<String, String> forcedProperties() {
        return Map.of("quarkus.container-image.build", "true",
                "quarkus.container-image.builder", builder.name());
    }

    @TaskAction
    public void checkRequiredExtensions() {
        // Currently forcedDependencies() is not implemented for gradle.
        // So, let's give users a meaningful warning message.
        String requiredExtension = "quarkus-container-image-" + builder.name();
        String requiredDependency = requiredExtension + "-deployment";
        List<String> projectDependencies = getProject().getConfigurations().stream().flatMap(c -> c.getDependencies().stream())
                .map(d -> d.getName())
                .collect(Collectors.toList());

        if (!projectDependencies.contains(requiredDependency)) {
            getProject().getLogger().warn("Task: {} requires extensions: {}", getName(), requiredDependency);
            getProject().getLogger().warn("To add the extensions to the project you can run the following command:");
            getProject().getLogger().warn("\tgradle addExtension --extensions={}", requiredExtension);
        }
    }
}
