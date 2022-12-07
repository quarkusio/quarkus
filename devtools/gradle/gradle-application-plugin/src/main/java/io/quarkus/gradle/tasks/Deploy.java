package io.quarkus.gradle.tasks;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.StopExecutionException;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

public abstract class Deploy extends QuarkusBuildProviderTask {

    public enum Deployer {

        kubernetes("quarkus-kubernetes", "quarkus-container-image-docker", "quarkus-container-image-jib",
                "quarkus-container-image-buildpack"),
        knative("quarkus-kubernetes", "quarkus-container-image-docker", "quarkus-container-image-jib",
                "quarkus-container-image-buildpack"),
        openshift("quarkus-openshift");

        private final String extension;
        private final String[] requiresOneOf;

        Deployer(String extension, String... requiresOneOf) {
            this.extension = extension;
            this.requiresOneOf = requiresOneOf;
        }

        public String getExtension() {
            return extension;
        }

        public String[] getRequiresOneOf() {
            return requiresOneOf;
        }
    }

    @Inject
    public Deploy(QuarkusBuildConfiguration buildConfiguration) {
        super(buildConfiguration, "Deploy");
    }

    @Input
    Optional<String> deployer = Optional.empty();
    boolean imageBuild = false;
    Optional<String> imageBuilder = Optional.empty();

    @Option(option = "deployer", description = "The deployer to use")
    public void setDeployer(String deployer) {
        this.deployer = Optional.ofNullable(deployer);
    }

    @Option(option = "image-build", description = "Perform an image build before deployment")
    public void setImageBuild(boolean imageBuild) {
        this.imageBuild = imageBuild;
    }

    @Option(option = "image-builder", description = "Perform an image build using the selected builder before deployment")
    public void setImageBuilder(String imageBuilder) {
        this.imageBuilder = Optional.ofNullable(imageBuilder);
        this.imageBuild = true;
    }

    public Deploy(QuarkusBuildConfiguration buildConfiguration, String description) {
        super(buildConfiguration, description);
    }

    @Override
    public Map<String, String> forcedProperties() {
        Map<String, String> props = new HashMap<>();
        props.put("quarkus." + getDeployer().name() + ".deploy", "true");
        props.put("quarkus.container-image.build", String.valueOf(imageBuilder.isPresent() || imageBuild));
        imageBuilder.ifPresent(b -> {
            props.put("quarkus.container-image.builder", b);
        });
        return props;
    }

    @TaskAction
    public void checkRequiredExtensions() {
        // Currently forcedDependencies() is not implemented for gradle.
        // So, let's give users a meaningful warning message.
        Deployer deployer = getDeployer();
        String requiredDeployerExtension = deployer.getExtension();
        Optional<String> requiredContainerImageExtension = imageBuilder.map(b -> "quarkus-container-image-" + b);
        List<String> projectDependencies = getProject().getConfigurations().stream().flatMap(c -> c.getDependencies().stream())
                .map(d -> d.getName())
                .collect(Collectors.toList());

        if (!projectDependencies.contains(requiredDeployerExtension)) {
            getProject().getLogger().warn("Task: {} requires extensions: {}", getName(), requiredDeployerExtension);
            getProject().getLogger().warn("To add the extensions to the project you can run the following command:");
            getProject().getLogger().warn("\tgradle addExtension --extensions={}", requiredDeployerExtension);
        } else if (!requiredContainerImageExtension.map(b -> projectDependencies.stream().anyMatch(d -> d.equals(b)))
                .orElse(true)) {
            abort("Task: {} using: {} is explicitly configured with missing container image builder extension: {}. Aborting.",
                    getName(), deployer.name(), requiredContainerImageExtension.get());
        } else if (imageBuild && deployer.getRequiresOneOf().length > 0) {
            List<String> unsatisfied = Arrays.stream(deployer.requiresOneOf)
                    .filter(r -> !projectDependencies.stream().anyMatch(d -> d.equals(r)))
                    .collect(Collectors.toList());
            if (unsatisfied.size() == deployer.getRequiresOneOf().length) {
                abort("Task: {} using: {} requires one of the following container image extensions: {}. Aborting.", getName(),
                        deployer.name(), unsatisfied.stream().collect(Collectors.joining(", ", "[", "]")));
            }
        }
    }

    public Deployer getDeployer() {
        return deployer.or(() -> DeploymentUtil.getEnabledDeployer()).map(d -> Deployer.valueOf(d)).orElse(Deployer.kubernetes);
    }

    private void abort(String message, Object... args) {
        getProject().getLogger().warn(message, args);
        getProject().getTasks().stream().filter(t -> t != this).forEach(t -> {
            t.setEnabled(false);
        });
        throw new StopExecutionException();
    }
}
