package io.quarkus.kubernetes.deployment;

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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.dekorate.Session;
import io.dekorate.SessionReader;
import io.dekorate.SessionWriter;
import io.dekorate.kubernetes.config.Configurator;
import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.logger.NoopLogger;
import io.dekorate.processor.SimpleFileReader;
import io.dekorate.processor.SimpleFileWriter;
import io.dekorate.project.Project;
import io.dekorate.utils.Maps;
import io.dekorate.utils.Strings;
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
import io.quarkus.kubernetes.spi.ConfiguratorBuildItem;
import io.quarkus.kubernetes.spi.CustomProjectRootBuildItem;
import io.quarkus.kubernetes.spi.DecoratorBuildItem;
import io.quarkus.kubernetes.spi.KubernetesDeploymentTargetBuildItem;
import io.quarkus.kubernetes.spi.KubernetesPortBuildItem;
import io.quarkus.runtime.LaunchMode;

class KubernetesProcessor {

    private static final Logger log = Logger.getLogger(KubernetesProcessor.class);

    private static final String OUTPUT_ARTIFACT_FORMAT = "%s%s.jar";
    public static final String DEFAULT_HASH_ALGORITHM = "SHA-256";

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
                entries.add(new DeploymentTargetEntry(deploymentTarget.getName(),
                        deploymentTarget.getKind(), deploymentTarget.getPriority()));
            }
        }
        return new EnabledKubernetesDeploymentTargetsBuildItem(entries);
    }

    @BuildStep(onlyIfNot = IsTest.class)
    public void build(ApplicationInfoBuildItem applicationInfo,
            OutputTargetBuildItem outputTarget,
            PackageConfig packageConfig,
            KubernetesConfig kubernetesConfig,
            OpenshiftConfig openshiftConfig,
            KnativeConfig knativeConfig,
            Capabilities capabilities,
            LaunchModeBuildItem launchMode,
            List<KubernetesPortBuildItem> kubernetesPorts,
            EnabledKubernetesDeploymentTargetsBuildItem kubernetesDeploymentTargets,
            List<ConfiguratorBuildItem> configurators,
            List<DecoratorBuildItem> decorators,
            Optional<CustomProjectRootBuildItem> customProjectRoot,
            BuildProducer<GeneratedFileSystemResourceBuildItem> generatedResourceProducer) {

        List<ConfiguratorBuildItem> allConfigurationRegistry = new ArrayList<>(configurators);
        List<DecoratorBuildItem> allDecorators = new ArrayList<>(decorators);

        if (kubernetesPorts.isEmpty()) {
            log.debug("The service is not an HTTP service so no Kubernetes manifests will be generated");
            return;
        }

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

        Path artifactPath = outputTarget.getOutputDirectory()
                .resolve(String.format(OUTPUT_ARTIFACT_FORMAT, outputTarget.getBaseName(), packageConfig.runnerSuffix));

        try {
            // by passing false to SimpleFileWriter, we ensure that no files are actually written during this phase
            Optional<Project> optionalProject = KubernetesCommonHelper.createProject(applicationInfo, customProjectRoot,
                    artifactPath);
            optionalProject.ifPresent(project -> {

                final Map<String, String> generatedResourcesMap;
                final SessionWriter sessionWriter = new SimpleFileWriter(project, false);
                final SessionReader sessionReader = new SimpleFileReader(
                        project.getRoot().resolve("src").resolve("main").resolve("kubernetes"), kubernetesDeploymentTargets
                                .getEntriesSortedByPriority().stream()
                                .map(DeploymentTargetEntry::getName).collect(Collectors.toSet()));
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
                allConfigurationRegistry.stream().filter(d -> d.matches(Configurator.class)).forEach(i -> {
                    Configurator configurator = (Configurator) i.getConfigurator();
                    session.getConfigurationRegistry().add(configurator);
                });

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

                // write the generated resources to the filesystem
                generatedResourcesMap = session.close();
                List<String> generatedFileNames = new ArrayList<>(generatedResourcesMap.size());
                for (Map.Entry<String, String> resourceEntry : generatedResourcesMap.entrySet()) {
                    Path path = Paths.get(resourceEntry.getKey());
                    //We need to ignore the config yml
                    if (!path.toFile().getParentFile().getName().equals("dekorate")) {
                        continue;
                    }
                    String fileName = path.toFile().getName();
                    Path targetPath = outputTarget.getOutputDirectory().resolve(KUBERNETES).resolve(fileName);
                    String relativePath = targetPath.toAbsolutePath().toString().replace(root.toAbsolutePath().toString(), "");

                    resourceEntry.getKey().replace(root.toAbsolutePath().toString(), KUBERNETES);
                    if (fileName.endsWith(".yml") || fileName.endsWith(".json")) {
                        String target = fileName.substring(0, fileName.lastIndexOf("."));
                        if (!deploymentTargets.contains(target)) {
                            continue;
                        }
                    }

                    generatedFileNames.add(fileName);
                    generatedResourceProducer.produce(
                            new GeneratedFileSystemResourceBuildItem(
                                    // we need to make sure we are only passing the relative path to the build item
                                    relativePath,
                                    resourceEntry.getValue().getBytes(StandardCharsets.UTF_8)));
                }

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
}
