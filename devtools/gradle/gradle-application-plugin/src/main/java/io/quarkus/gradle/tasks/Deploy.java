package io.quarkus.gradle.tasks;

import static io.quarkus.gradle.tasks.QuarkusBuild.QUARKUS_IGNORE_LEGACY_DEPLOY_BUILD;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.deployment.cmd.DeployCommandActionResultBuildItem;
import io.quarkus.deployment.cmd.DeployCommandDeclarationHandler;
import io.quarkus.deployment.cmd.DeployCommandDeclarationResultBuildItem;
import io.quarkus.deployment.cmd.DeployCommandHandler;
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
        super("Deploy", false);
    }

    @TaskAction
    public void checkRequiredExtensions() {
        ApplicationModel appModel = resolveAppModelForBuild();
        Properties sysProps = new Properties();
        sysProps.putAll(extension().buildEffectiveConfiguration(appModel).getValues());
        try (CuratedApplication curatedApplication = QuarkusBootstrap.builder()
                .setBaseClassLoader(getClass().getClassLoader())
                .setExistingModel(appModel)
                .setTargetDirectory(getProject().getLayout().getBuildDirectory().getAsFile().get().toPath())
                .setBaseName(extension().finalName())
                .setBuildSystemProperties(sysProps)
                .setAppArtifact(appModel.getAppArtifact())
                .setLocalProjectDiscovery(false)
                .setIsolateDeployment(true)
                .build().bootstrap()) {
            AtomicReference<List<String>> tooMany = new AtomicReference<>();
            AugmentAction action = curatedApplication.createAugmentor();
            action.performCustomBuild(DeployCommandDeclarationHandler.class.getName(), new Consumer<List<String>>() {
                @Override
                public void accept(List<String> strings) {
                    tooMany.set(strings);
                }
            }, DeployCommandDeclarationResultBuildItem.class.getName());
            String target = System.getProperty("quarkus.deploy.target");
            List<String> targets = tooMany.get();
            if (targets.isEmpty() && target == null) {
                // Currently forcedDependencies() is not implemented for gradle.
                // So, let's give users a meaningful warning message.
                Deployer deployer = getDeployer();
                extension().forcedPropertiesProperty().convention(
                        getProject().provider(() -> {
                            Map<String, String> props = new HashMap<>();
                            props.put("quarkus." + deployer.name() + ".deploy", "true");
                            props.put("quarkus.container-image.build", String.valueOf(imageBuilder.isPresent() || imageBuild));
                            imageBuilder.ifPresent(b -> props.put("quarkus.container-image.builder", b));
                            return props;
                        }));
                String requiredDeployerExtension = deployer.getExtension();
                Optional<String> requiredContainerImageExtension = requiredContainerImageExtension();

                List<String> projectDependencies = getProject().getConfigurations().stream()
                        .flatMap(c -> c.getDependencies().stream())
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
                        abort("Task: {} using: {} requires one of the following container image extensions: {}. Aborting.",
                                getName(),
                                deployer.name(), unsatisfied.stream().collect(Collectors.joining(", ", "[", "]")));
                    }
                }
                return;
            } else if (targets.size() > 1 && target == null) {
                abort(
                        "Too many installed extensions support quarkus:deploy.  You must choose one by setting quarkus.deploy.target."
                                + " Extensions: " + targets.stream().collect(Collectors.joining(" ")));
            } else if (target != null && !targets.contains(target)) {
                abort(
                        "Unknown quarkus.deploy.target: " + target + " Extensions: "
                                + targets.stream().collect(Collectors.joining(" ")));
            } else {
                extension().forcedPropertiesProperty().convention(
                        getProject().provider(() -> {
                            Map<String, String> props = new HashMap<>();
                            props.put(QUARKUS_IGNORE_LEGACY_DEPLOY_BUILD, "true");
                            return props;
                        }));
                if (target == null) {
                    target = targets.get(0);
                }
                AugmentAction deployAction = curatedApplication.createAugmentor();
                getLogger().info("Deploy target: " + target);
                System.setProperty("quarkus.deploy.target", target);

                deployAction.performCustomBuild(DeployCommandHandler.class.getName(), new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean success) {
                    }
                }, DeployCommandActionResultBuildItem.class.getName());
            }
        } catch (BootstrapException ex) {
            getLogger().error(ex.getMessage(), ex);
            abort("Failed to run deploy");
            return;
        }
    }

    public Deployer getDeployer() {
        return getDeployer(Deployer.kubernetes);
    }

    public Deployer getDeployer(Deployer defaultDeployer) {
        return deployer
                .or(() -> DeploymentUtil.getEnabledDeployer())
                .or(() -> getProjectDeployers().stream().findFirst())
                .map(Deployer::valueOf)
                .orElse(defaultDeployer);
    }

    public Optional<String> requiredContainerImageExtension() {
        return imageBuilder.map(b -> "quarkus-container-image-" + b)
                .or(() -> imageBuild ? Arrays.stream(getDeployer().requiresOneOf).findFirst() : Optional.empty());
    }

    private Set<String> getProjectDeployers() {
        return getProject().getConfigurations().stream().flatMap(c -> c.getDependencies().stream())
                .map(d -> d.getName())
                .filter(d -> Arrays.stream(Deployer.values()).map(Deployer::getExtension).anyMatch(e -> d.equals(e)))
                .map(d -> d.replaceAll("^quarkus\\-", ""))
                .collect(Collectors.toSet());
    }

}
