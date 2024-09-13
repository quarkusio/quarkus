
package io.quarkus.gradle.tasks;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.gradle.api.tasks.TaskAction;

import io.quarkus.gradle.dependency.ApplicationDeploymentClasspathBuilder;
import io.quarkus.gradle.tooling.ToolingUtils;
import io.quarkus.runtime.LaunchMode;

public abstract class ImageTask extends QuarkusBuildTask {

    private static final String DEPLOYMENT_SUFFIX = "-deployment";
    static final String QUARKUS_PREFIX = "quarkus-";
    static final String QUARKUS_CONTAINER_IMAGE_PREFIX = "quarkus-container-image-";
    static final String QUARKUS_CONTAINER_IMAGE_BUILD = "quarkus.container-image.build";
    static final String QUARKUS_CONTAINER_IMAGE_PUSH = "quarkus.container-image.push";
    static final String QUARKUS_CONTAINER_IMAGE_BUILDER = "quarkus.container-image.builder";

    static final Map<String, Builder> BUILDERS = new HashMap<>();
    static {
        for (Builder builder : Builder.values()) {
            BUILDERS.put(builder.name(), builder);
        }
    }

    enum Builder {
        docker,
        jib,
        buildpack,
        openshift
    }

    public ImageTask(String description) {
        super(description, false);
    }

    public Builder builder() {
        return builderFromSystemProperties()
                .or(() -> availableBuilders().stream().findFirst())
                .orElse(Builder.docker);
    }

    Optional<Builder> builderFromSystemProperties() {
        return Optional.ofNullable(System.getProperty(QUARKUS_CONTAINER_IMAGE_BUILDER))
                .filter(BUILDERS::containsKey)
                .map(BUILDERS::get);
    }

    List<Builder> availableBuilders() {
        // This will only pickup direct dependencies and not transitives
        // This means that extensions like quarkus-container-image-openshift via quarkus-openshift are not picked up
        // So, let's relax our filters a bit so that we can pickup quarkus-openshift directly (relax the prefix requirement).
        return getProject().getConfigurations()
                .getByName(ToolingUtils.toDeploymentConfigurationName(
                        ApplicationDeploymentClasspathBuilder.getFinalRuntimeConfigName(LaunchMode.NORMAL)))
                .getDependencies().stream()
                .map(d -> d.getName())
                .filter(n -> n.startsWith(QUARKUS_CONTAINER_IMAGE_PREFIX) || n.startsWith(QUARKUS_PREFIX))
                .map(n -> n.replace(QUARKUS_CONTAINER_IMAGE_PREFIX, "").replace(QUARKUS_PREFIX, "").replace(DEPLOYMENT_SUFFIX,
                        ""))
                .filter(BUILDERS::containsKey)
                .map(BUILDERS::get)
                .collect(Collectors.toList());
    }

    @TaskAction
    public void checkRequiredExtensions() {
        // Currently forcedDependencies() is not implemented for gradle.
        // So, let's give users a meaningful warning message.
        List<Builder> availableBuidlers = availableBuilders();
        Optional<Builder> missingBuilder = Optional.of(builder()).filter(Predicate.not(availableBuidlers::contains));
        missingBuilder.map(builder -> QUARKUS_CONTAINER_IMAGE_PREFIX + builder.name()).ifPresent(missingDependency -> {
            getLogger().warn("Task: {} requires extensions: {}", getName(), missingDependency);
            getLogger().warn("To add the extensions to the project you can run the following command:");
            getLogger().warn("\tgradle addExtension --extensions={}", missingDependency);
            abort("Aborting.");
        });

        if (!missingBuilder.isPresent() && availableBuidlers.isEmpty()) {
            getLogger().warn("Task: {} requires on of extensions: {}", getName(),
                    Arrays.stream(Builder.values()).map(Builder::name).collect(Collectors.joining(", ", "[", "]")));
            getLogger().warn("To add the extensions to the project you can run the following command:");
            getLogger().warn("\tgradle addExtension --extensions=<extension name>");
            abort("Aborting.");
        }
    }
}
