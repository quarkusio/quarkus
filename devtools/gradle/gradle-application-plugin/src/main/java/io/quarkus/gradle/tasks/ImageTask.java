
package io.quarkus.gradle.tasks;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.gradle.api.tasks.StopExecutionException;
import org.gradle.api.tasks.TaskAction;

public abstract class ImageTask extends QuarkusBuildProviderTask {

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

    public ImageTask(QuarkusBuildConfiguration buildConfiguration, String description) {
        super(buildConfiguration, description);
    }

    @Override
    public Map<String, String> forcedProperties() {
        return Map.of(QUARKUS_CONTAINER_IMAGE_BUILD, "true");
    }

    Optional<Builder> builder() {
        return builderFromSystemProperties();
    }

    Optional<Builder> builderFromSystemProperties() {
        return Optional.ofNullable(System.getProperty(QUARKUS_CONTAINER_IMAGE_BUILDER))
                .filter(BUILDERS::containsKey)
                .map(BUILDERS::get);
    }

    List<Builder> availableBuilders() {
        return getProject().getConfigurations().stream().flatMap(c -> c.getDependencies().stream())
                .map(d -> d.getName())
                .filter(n -> n.startsWith(QUARKUS_CONTAINER_IMAGE_PREFIX))
                .map(n -> n.replace(QUARKUS_CONTAINER_IMAGE_PREFIX, ""))
                .filter(BUILDERS::containsKey)
                .map(BUILDERS::get)
                .collect(Collectors.toList());
    }

    @TaskAction
    public void checkRequiredExtensions() {
        // Currently forcedDependencies() is not implemented for gradle.
        // So, let's give users a meaningful warning message.
        List<Builder> availableBuidlers = availableBuilders();
        Optional<Builder> missingBuilder = builder().filter(Predicate.not(availableBuidlers::contains));
        missingBuilder.map(builder -> QUARKUS_CONTAINER_IMAGE_PREFIX + builder.name()).ifPresent(missingDependency -> {
            getProject().getLogger().warn("Task: {} requires extensions: {}", getName(), missingDependency);
            getProject().getLogger().warn("To add the extensions to the project you can run the following command:");
            getProject().getLogger().warn("\tgradle addExtension --extensions={}", missingDependency);
            abort("Aborting.");
        });

        if (!missingBuilder.isPresent() && availableBuidlers.isEmpty()) {
            getProject().getLogger().warn("Task: {} requires on of extensions: {}", getName(),
                    Arrays.stream(Builder.values()).map(Builder::name).collect(Collectors.joining(", ", "[", "]")));
            getProject().getLogger().warn("To add the extensions to the project you can run the following command:");
            getProject().getLogger().warn("\tgradle addExtension --extensions=<extension name>");
            abort("Aborting.");
        }
    }

    void abort(String message, Object... args) {
        getProject().getLogger().warn(message, args);
        getProject().getTasks().stream().filter(t -> t != this).forEach(t -> {
            t.setEnabled(false);
        });
        throw new StopExecutionException();
    }
}
