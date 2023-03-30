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

import io.quarkus.deployment.util.DeploymentUtil;

public abstract class Deploy extends QuarkusBuildTask {

    public enum Deployer {

        kubernetes("quarkus-kubernetes", "quarkus-container-image-docker", "quarkus-container-image-jib",
                "quarkus-container-image-buildpack"),
        minikube("quarkus-minikube", "quarkus-container-image-docker", "quarkus-container-image-jib",
                "quarkus-container-image-buildpack"),
        kind("quarkus-kind", "quarkus-container-image-docker", "quarkus-container-image-jib",
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

    @Inject
    public Deploy() {
        super("Deploy");
        extension().forcedPropertiesProperty().convention(
                getProject().provider(() -> {
                    Map<String, String> props = new HashMap<>();
                    props.put("quarkus." + getDeployer().name() + ".deploy", "true");
                    props.put("quarkus.container-image.build", String.valueOf(imageBuilder.isPresent() || imageBuild));
                    imageBuilder.ifPresent(b -> props.put("quarkus.container-image.builder", b));
                    return props;
                }));
    }

    @TaskAction
    public void checkRequiredExtensions() {
        // Currently forcedDependencies() is not implemented for gradle.
        // So, let's give users a meaningful warning message.
        Deployer deployer = getDeployer();
        String requiredDeployerExtension = deployer.getExtension();
        Optional<String> requiredContainerImageExtension = requiredContainerImageExtension();

        List<String> projectDependencies = getProject().getConfigurations().stream().flatMap(c -> c.getDependencies().stream())
                .map(d -> d.getName())
                .collect(Collectors.toList());
        if (!projectDependencies.contains(requiredDeployerExtension)) {
            abort("Task: {} requires extensions: {}\n" +
                    "To add the extensions to the project you can run the following command:\n" +
                    "\tgradle addExtension --extensions={}",
                    getName(), requiredDeployerExtension,
                    requiredDeployerExtension);
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

    public Optional<String> requiredContainerImageExtension() {
        return imageBuilder.map(b -> "quarkus-container-image-" + b)
                .or(() -> imageBuild ? Arrays.stream(getDeployer().requiresOneOf).findFirst() : Optional.empty());
    }

    }
}
