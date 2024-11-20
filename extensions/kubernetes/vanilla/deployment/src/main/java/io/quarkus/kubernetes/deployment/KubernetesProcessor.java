package io.quarkus.kubernetes.deployment;

import static io.quarkus.deployment.pkg.steps.JarResultBuildStep.DEFAULT_FAST_JAR_DIRECTORY_NAME;
import static io.quarkus.deployment.pkg.steps.JarResultBuildStep.QUARKUS_RUN_JAR;
import static io.quarkus.kubernetes.deployment.Constants.KUBERNETES;
import static io.quarkus.kubernetes.spi.KubernetesDeploymentTargetBuildItem.mergeList;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.dekorate.Session;
import io.dekorate.SessionReader;
import io.dekorate.SessionWriter;
import io.dekorate.config.ConfigurationSupplier;
import io.dekorate.kubernetes.config.Configurator;
import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.logger.NoopLogger;
import io.dekorate.processor.SimpleFileReader;
import io.dekorate.project.Project;
import io.dekorate.utils.Maps;
import io.dekorate.utils.Strings;
import io.quarkus.container.image.deployment.ContainerImageConfig;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsTest;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedFileSystemResourceBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.util.FileUtil;
import io.quarkus.kubernetes.spi.ConfigurationSupplierBuildItem;
import io.quarkus.kubernetes.spi.ConfiguratorBuildItem;
import io.quarkus.kubernetes.spi.CustomKubernetesOutputDirBuildItem;
import io.quarkus.kubernetes.spi.CustomProjectRootBuildItem;
import io.quarkus.kubernetes.spi.DecoratorBuildItem;
import io.quarkus.kubernetes.spi.DekorateOutputBuildItem;
import io.quarkus.kubernetes.spi.GeneratedKubernetesResourceBuildItem;
import io.quarkus.kubernetes.spi.KubernetesDeploymentTargetBuildItem;
import io.quarkus.kubernetes.spi.KubernetesOutputDirectoryBuildItem;
import io.quarkus.kubernetes.spi.KubernetesPortBuildItem;
import io.quarkus.runtime.LaunchMode;

class KubernetesProcessor {

    private static final Logger log = Logger.getLogger(KubernetesProcessor.class);

    private static final String COMMON = "common";

    @BuildStep
    FeatureBuildItem produceFeature() {
        return new FeatureBuildItem(Feature.KUBERNETES);
    }

    @BuildStep
    public EnabledKubernetesDeploymentTargetsBuildItem enabledKubernetesDeploymentTargets(
            List<KubernetesDeploymentTargetBuildItem> allDeploymentTargets) {
        List<KubernetesDeploymentTargetBuildItem> mergedDeploymentTargets = mergeList(allDeploymentTargets);
        Collections.sort(mergedDeploymentTargets);

        List<DeploymentTargetEntry> entries = new ArrayList<>(mergedDeploymentTargets.size());
        for (KubernetesDeploymentTargetBuildItem deploymentTarget : mergedDeploymentTargets) {
            if (deploymentTarget.isEnabled()) {
                DeploymentResourceKind deploymentResourceKind = DeploymentResourceKind.find(deploymentTarget.getGroup(),
                        deploymentTarget.getVersion(), deploymentTarget.getKind());
                entries.add(new DeploymentTargetEntry(deploymentTarget.getName(), deploymentResourceKind,
                        deploymentTarget.getPriority(), deploymentTarget.getDeployStrategy()));
            }
        }
        return new EnabledKubernetesDeploymentTargetsBuildItem(entries);
    }

    @BuildStep
    public void preventContainerPush(ContainerImageConfig containerImageConfig,
            BuildProducer<PreventImplicitContainerImagePushBuildItem> producer) {
        if (containerImageConfig.isPushExplicitlyDisabled()) {
            producer.produce(new PreventImplicitContainerImagePushBuildItem());
        }
    }

    @BuildStep(onlyIfNot = IsTest.class)
    public void build(ApplicationInfoBuildItem applicationInfo,
            OutputTargetBuildItem outputTarget,
            PackageConfig packageConfig,
            KubernetesConfig kubernetesConfig,
            OpenShiftConfig openshiftConfig,
            KnativeConfig knativeConfig,
            Capabilities capabilities,
            LaunchModeBuildItem launchMode,
            List<KubernetesPortBuildItem> kubernetesPorts,
            EnabledKubernetesDeploymentTargetsBuildItem kubernetesDeploymentTargets,
            List<ConfiguratorBuildItem> configurators,
            List<ConfigurationSupplierBuildItem> configurationSuppliers,
            List<DecoratorBuildItem> decorators,
            BuildProducer<DekorateOutputBuildItem> dekorateSessionProducer,
            Optional<CustomProjectRootBuildItem> customProjectRoot,
            Optional<CustomKubernetesOutputDirBuildItem> customOutputDir,
            BuildProducer<GeneratedFileSystemResourceBuildItem> generatedResourceProducer,
            BuildProducer<GeneratedKubernetesResourceBuildItem> generatedKubernetesResourceProducer,
            BuildProducer<KubernetesOutputDirectoryBuildItem> outputDirectoryBuildItemBuildProducer) {

        List<ConfiguratorBuildItem> allConfigurators = new ArrayList<>(configurators);
        List<ConfigurationSupplierBuildItem> allConfigurationSuppliers = new ArrayList<>(configurationSuppliers);
        List<DecoratorBuildItem> allDecorators = new ArrayList<>(decorators);

        final Path root;
        try {
            root = Files.createTempDirectory("quarkus-kubernetes");
        } catch (IOException e) {
            throw new RuntimeException("Unable to setup environment for generating Kubernetes resources", e);
        }

        Map<String, Object> config = KubernetesConfigUtil.toMap(kubernetesConfig, openshiftConfig, knativeConfig);
        Set<String> deploymentTargets = kubernetesDeploymentTargets.getEntriesSortedByPriority().stream()
                .map(DeploymentTargetEntry::getName)
                .collect(Collectors.toSet());

        Path artifactPath = getRunner(outputTarget, packageConfig);

        try {
            // by passing false to SimpleFileWriter, we ensure that no files are actually written during this phase
            Optional<Project> optionalProject = KubernetesCommonHelper.createProject(applicationInfo, customProjectRoot,
                    artifactPath);
            optionalProject.ifPresent(project -> {
                Set<String> targets = new HashSet<>();
                targets.add(COMMON);
                targets.addAll(deploymentTargets);
                final Map<String, String> generatedResourcesMap;
                final SessionWriter sessionWriter = new QuarkusFileWriter(project);
                final SessionReader sessionReader = new SimpleFileReader(
                        project.getRoot().resolve("src").resolve("main").resolve("kubernetes"), targets);
                sessionWriter.setProject(project);

                if (launchMode.getLaunchMode() != LaunchMode.NORMAL) {
                    // needed for a fresh run
                    Session.clearSession();
                }

                final Session session = Session.getSession(new NoopLogger());

                session.setWriter(sessionWriter);
                session.setReader(sessionReader);

                session.addPropertyConfiguration(Maps.fromProperties(config));

                //We need to verify to filter out anything that doesn't extend the Configurator class.
                //The ConfiguratorBuildItem is a wrapper to Object.
                for (ConfiguratorBuildItem configuratorBuildItem : allConfigurators) {
                    session.getConfigurationRegistry().add((Configurator) configuratorBuildItem.getConfigurator());
                }
                //We need to verify to filter out anything that doesn't extend the ConfigurationSupplier class.
                //The ConfigurationSupplierBuildItem is a wrapper to Object.
                for (ConfigurationSupplierBuildItem configurationSupplierBuildItem : allConfigurationSuppliers) {
                    session.getConfigurationRegistry()
                            .add((ConfigurationSupplier) configurationSupplierBuildItem.getConfigurationSupplier());
                }

                //We need to verify to filter out anything that doesn't extend the Decorator class.
                //The DecoratorBuildItem is a wrapper to Object.
                allDecorators.stream().filter(d -> d.matches(Decorator.class)).forEach(i -> {
                    String group = i.getGroup();
                    Decorator decorator = (Decorator) i.getDecorator();
                    if (Strings.isNullOrEmpty(group)) {
                        session.getResourceRegistry().decorate(decorator);
                    } else {
                        session.getResourceRegistry().decorate(group, decorator);
                    }
                });

                //The targetDirectory should be the custom if provided, oterwise the 'default' output directory.
                //I this case 'default' means that one that we used until now (up until we introduced the ability to override).
                Path targetDirectory = customOutputDir
                        .map(c -> c.getOutputDir())
                        .map(d -> d.isAbsolute() ? d : project.getRoot().resolve(d))
                        .orElseGet(() -> getEffectiveOutputDirectory(kubernetesConfig, project.getRoot(),
                                outputTarget.getOutputDirectory()));

                outputDirectoryBuildItemBuildProducer.produce(new KubernetesOutputDirectoryBuildItem(targetDirectory));

                // write the generated resources to the filesystem
                generatedResourcesMap = session.close();
                List<String> generatedFiles = new ArrayList<>(generatedResourcesMap.size());
                List<String> generatedFileNames = new ArrayList<>(generatedResourcesMap.size());
                for (Map.Entry<String, String> resourceEntry : generatedResourcesMap.entrySet()) {
                    Path path = Paths.get(resourceEntry.getKey());
                    //We need to ignore the config yml
                    if (!path.toFile().getParentFile().getName().equals("dekorate")) {
                        continue;
                    }
                    String fileName = path.toFile().getName();
                    Path targetPath = targetDirectory.resolve(fileName);
                    String relativePath = targetPath.toAbsolutePath().toString().replace(root.toAbsolutePath().toString(), "");

                    generatedKubernetesResourceProducer.produce(new GeneratedKubernetesResourceBuildItem(fileName,
                            resourceEntry.getValue().getBytes(StandardCharsets.UTF_8)));

                    if (fileName.endsWith(".yml") || fileName.endsWith(".json")) {
                        String target = fileName.substring(0, fileName.lastIndexOf("."));
                        if (!deploymentTargets.contains(target)) {
                            continue;
                        }
                    }

                    generatedFileNames.add(fileName);
                    generatedFiles.add(relativePath);
                    generatedResourceProducer.produce(
                            new GeneratedFileSystemResourceBuildItem(
                                    // we need to make sure we are only passing the relative path to the build item
                                    relativePath,
                                    resourceEntry.getValue().getBytes(StandardCharsets.UTF_8)));
                }

                dekorateSessionProducer.produce(new DekorateOutputBuildItem(project, session, generatedFiles));

                if (!generatedFileNames.isEmpty()) {
                    log.debugf("Generated the Kubernetes manifests: '%s' in '%s'", String.join(",", generatedFileNames),
                            outputTarget.getOutputDirectory() + File.separator + KUBERNETES);
                }

                try {
                    if (root != null && root.toFile().exists()) {
                        FileUtil.deleteDirectory(root);
                    }
                } catch (IOException e) {
                    log.debug("Unable to delete temporary directory " + root, e);
                }

            });

            if (!optionalProject.isPresent()) {
                log.warn("No project was detected, skipping generation of kubernetes manifests!");
            }
        } catch (Exception e) {
            if (launchMode.getLaunchMode() == LaunchMode.NORMAL) {
                throw e;
            }

            log.warn("Failed to generate Kubernetes resources", e);
        }

    }

    /**
     * This method is based on the logic in {@link io.quarkus.deployment.pkg.steps.JarResultBuildStep#buildRunnerJar}.
     * Note that we cannot consume the {@link io.quarkus.deployment.pkg.builditem.JarBuildItem} because it causes build cycle
     * exceptions since we need to support adding generated resources into the JAR file (see
     * https://github.com/quarkusio/quarkus/pull/20113).
     */
    private Path getRunner(OutputTargetBuildItem outputTarget,
            PackageConfig packageConfig) {
        PackageConfig.JarConfig.JarType jarType = packageConfig.jar().type();
        return switch (jarType) {
            case LEGACY_JAR, UBER_JAR -> outputTarget.getOutputDirectory()
                    .resolve(outputTarget.getBaseName() + packageConfig.computedRunnerSuffix() + ".jar");
            case FAST_JAR, MUTABLE_JAR -> {
                //thin JAR
                Path buildDir;

                if (packageConfig.outputDirectory().isPresent()) {
                    buildDir = outputTarget.getOutputDirectory();
                } else {
                    buildDir = outputTarget.getOutputDirectory().resolve(DEFAULT_FAST_JAR_DIRECTORY_NAME);
                }

                yield buildDir.resolve(QUARKUS_RUN_JAR);
            }
        };
    }

    /**
     * Resolve the effective output directory where to generate the Kubernetes manifests.
     * If the `quarkus.kubernetes.output-directory` property is not provided, then the default project output directory will be
     * used.
     *
     * @param config The Kubernetes configuration.
     * @param projectLocation The project location.
     * @param projectOutputDirectory The project output target.
     * @return the effective output directory.
     */
    private Path getEffectiveOutputDirectory(KubernetesConfig config, Path projectLocation, Path projectOutputDirectory) {
        return config.outputDirectory().map(d -> projectLocation.resolve(d))
                .orElse(projectOutputDirectory.resolve(KUBERNETES));
    }
}
