package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.DEFAULT_HTTP_PORT;
import static io.quarkus.kubernetes.deployment.Constants.DEFAULT_S2I_IMAGE_NAME;
import static io.quarkus.kubernetes.deployment.Constants.DEPLOYMENT;
import static io.quarkus.kubernetes.deployment.Constants.DEPLOYMENT_CONFIG;
import static io.quarkus.kubernetes.deployment.Constants.HTTP_PORT;
import static io.quarkus.kubernetes.deployment.Constants.KNATIVE;
import static io.quarkus.kubernetes.deployment.Constants.KUBERNETES;
import static io.quarkus.kubernetes.deployment.Constants.MAX_NODE_PORT_VALUE;
import static io.quarkus.kubernetes.deployment.Constants.MAX_PORT_NUMBER;
import static io.quarkus.kubernetes.deployment.Constants.MINIKUBE;
import static io.quarkus.kubernetes.deployment.Constants.MIN_NODE_PORT_VALUE;
import static io.quarkus.kubernetes.deployment.Constants.MIN_PORT_NUMBER;
import static io.quarkus.kubernetes.deployment.Constants.OPENSHIFT;
import static io.quarkus.kubernetes.deployment.Constants.OPENSHIFT_APP_RUNTIME;
import static io.quarkus.kubernetes.deployment.Constants.QUARKUS;
import static io.quarkus.kubernetes.deployment.Constants.QUARKUS_ANNOTATIONS_BUILD_TIMESTAMP;
import static io.quarkus.kubernetes.deployment.Constants.QUARKUS_ANNOTATIONS_COMMIT_ID;
import static io.quarkus.kubernetes.deployment.Constants.QUARKUS_ANNOTATIONS_VCS_URL;
import static io.quarkus.kubernetes.deployment.Constants.SERVICE;
import static io.quarkus.kubernetes.deployment.ResourceNameUtil.*;
import static io.quarkus.kubernetes.spi.KubernetesDeploymentTargetBuildItem.DEFAULT_PRIORITY;
import static io.quarkus.kubernetes.spi.KubernetesDeploymentTargetBuildItem.VANILLA_KUBERNETES_PRIORITY;
import static io.quarkus.kubernetes.spi.KubernetesDeploymentTargetBuildItem.mergeList;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
import io.dekorate.knative.decorator.ApplyGlobalAutoscalingClassDecorator;
import io.dekorate.knative.decorator.ApplyGlobalContainerConcurrencyDecorator;
import io.dekorate.knative.decorator.ApplyGlobalRequestsPerSecondTargetDecorator;
import io.dekorate.knative.decorator.ApplyGlobalTargetUtilizationDecorator;
import io.dekorate.knative.decorator.ApplyLocalAutoscalingClassDecorator;
import io.dekorate.knative.decorator.ApplyLocalAutoscalingMetricDecorator;
import io.dekorate.knative.decorator.ApplyLocalAutoscalingTargetDecorator;
import io.dekorate.knative.decorator.ApplyLocalContainerConcurrencyDecorator;
import io.dekorate.knative.decorator.ApplyLocalTargetUtilizationPercentageDecorator;
import io.dekorate.knative.decorator.ApplyMaxScaleDecorator;
import io.dekorate.knative.decorator.ApplyMinScaleDecorator;
import io.dekorate.kubernetes.annotation.ImagePullPolicy;
import io.dekorate.kubernetes.annotation.ServiceType;
import io.dekorate.kubernetes.config.Annotation;
import io.dekorate.kubernetes.config.Configurator;
import io.dekorate.kubernetes.config.EnvBuilder;
import io.dekorate.kubernetes.config.ImageConfigurationFluent;
import io.dekorate.kubernetes.config.Label;
import io.dekorate.kubernetes.config.LabelBuilder;
import io.dekorate.kubernetes.config.PortBuilder;
import io.dekorate.kubernetes.configurator.AddPort;
import io.dekorate.kubernetes.decorator.AddAnnotationDecorator;
import io.dekorate.kubernetes.decorator.AddAwsElasticBlockStoreVolumeDecorator;
import io.dekorate.kubernetes.decorator.AddAzureDiskVolumeDecorator;
import io.dekorate.kubernetes.decorator.AddAzureFileVolumeDecorator;
import io.dekorate.kubernetes.decorator.AddConfigMapDataDecorator;
import io.dekorate.kubernetes.decorator.AddConfigMapResourceProvidingDecorator;
import io.dekorate.kubernetes.decorator.AddConfigMapVolumeDecorator;
import io.dekorate.kubernetes.decorator.AddEnvVarDecorator;
import io.dekorate.kubernetes.decorator.AddImagePullSecretDecorator;
import io.dekorate.kubernetes.decorator.AddInitContainerDecorator;
import io.dekorate.kubernetes.decorator.AddLabelDecorator;
import io.dekorate.kubernetes.decorator.AddLivenessProbeDecorator;
import io.dekorate.kubernetes.decorator.AddMountDecorator;
import io.dekorate.kubernetes.decorator.AddPvcVolumeDecorator;
import io.dekorate.kubernetes.decorator.AddReadinessProbeDecorator;
import io.dekorate.kubernetes.decorator.AddRoleBindingResourceDecorator;
import io.dekorate.kubernetes.decorator.AddSecretVolumeDecorator;
import io.dekorate.kubernetes.decorator.AddServiceAccountResourceDecorator;
import io.dekorate.kubernetes.decorator.ApplicationContainerDecorator;
import io.dekorate.kubernetes.decorator.ApplyArgsDecorator;
import io.dekorate.kubernetes.decorator.ApplyCommandDecorator;
import io.dekorate.kubernetes.decorator.ApplyImagePullPolicyDecorator;
import io.dekorate.kubernetes.decorator.ApplyServiceAccountNamedDecorator;
import io.dekorate.kubernetes.decorator.ApplyWorkingDirDecorator;
import io.dekorate.kubernetes.decorator.RemoveAnnotationDecorator;
import io.dekorate.logger.NoopLogger;
import io.dekorate.processor.SimpleFileReader;
import io.dekorate.processor.SimpleFileWriter;
import io.dekorate.project.BuildInfo;
import io.dekorate.project.FileProjectFactory;
import io.dekorate.project.Project;
import io.dekorate.project.ScmInfo;
import io.dekorate.s2i.config.S2iBuildConfig;
import io.dekorate.s2i.config.S2iBuildConfigBuilder;
import io.dekorate.s2i.config.S2iBuildConfigFluent;
import io.dekorate.s2i.decorator.AddBuilderImageStreamResourceDecorator;
import io.dekorate.utils.Annotations;
import io.dekorate.utils.Maps;
import io.quarkus.container.image.deployment.util.ImageUtil;
import io.quarkus.container.spi.BaseImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImageLabelBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedFileSystemResourceBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.util.FileUtil;
import io.quarkus.kubernetes.deployment.Annotations.Prometheus;
import io.quarkus.kubernetes.spi.KubernetesAnnotationBuildItem;
import io.quarkus.kubernetes.spi.KubernetesCommandBuildItem;
import io.quarkus.kubernetes.spi.KubernetesDeploymentTargetBuildItem;
import io.quarkus.kubernetes.spi.KubernetesEnvBuildItem;
import io.quarkus.kubernetes.spi.KubernetesHealthLivenessPathBuildItem;
import io.quarkus.kubernetes.spi.KubernetesHealthReadinessPathBuildItem;
import io.quarkus.kubernetes.spi.KubernetesLabelBuildItem;
import io.quarkus.kubernetes.spi.KubernetesPortBuildItem;
import io.quarkus.kubernetes.spi.KubernetesRoleBindingBuildItem;
import io.quarkus.kubernetes.spi.KubernetesRoleBuildItem;
import io.quarkus.runtime.LaunchMode;

class KubernetesProcessor {

    private static final Logger log = Logger.getLogger(KubernetesProcessor.class);

    private static final String OUTPUT_ARTIFACT_FORMAT = "%s%s.jar";
    public static final String DEFAULT_HASH_ALGORITHM = "SHA-256";

    private static final int MINIKUBE_PRIORITY = DEFAULT_PRIORITY + 20;
    private static final int OPENSHIFT_PRIORITY = DEFAULT_PRIORITY + 10;
    private static final int KNATIVE_PRIORITY = DEFAULT_PRIORITY;

    @BuildStep
    FeatureBuildItem produceFeature() {
        return new FeatureBuildItem(Feature.KUBERNETES);
    }

    @BuildStep
    public void deploymentTargets(BuildProducer<KubernetesDeploymentTargetBuildItem> deploymentTargets) {
        List<String> userSpecifiedDeploymentTargets = KubernetesConfigUtil.getUserSpecifiedDeploymentTargets();
        if (userSpecifiedDeploymentTargets.isEmpty()) {
            // when nothing was selected by the user, we enable vanilla Kubernetes by default
            deploymentTargets.produce(
                    new KubernetesDeploymentTargetBuildItem(KUBERNETES, DEPLOYMENT, VANILLA_KUBERNETES_PRIORITY, true));
        }

        // even if these are disabled, they serve the purpose of setting the proper priorities

        deploymentTargets
                .produce(new KubernetesDeploymentTargetBuildItem(MINIKUBE, DEPLOYMENT, MINIKUBE_PRIORITY,
                        userSpecifiedDeploymentTargets.contains(MINIKUBE)));

        deploymentTargets
                .produce(new KubernetesDeploymentTargetBuildItem(OPENSHIFT, DEPLOYMENT_CONFIG, OPENSHIFT_PRIORITY,
                        userSpecifiedDeploymentTargets.contains(OPENSHIFT)));

        deploymentTargets.produce(new KubernetesDeploymentTargetBuildItem(KNATIVE, SERVICE, KNATIVE_PRIORITY,
                userSpecifiedDeploymentTargets.contains(KNATIVE)));

        deploymentTargets.produce(
                new KubernetesDeploymentTargetBuildItem(KUBERNETES, DEPLOYMENT, VANILLA_KUBERNETES_PRIORITY,
                        userSpecifiedDeploymentTargets.contains(KUBERNETES)));
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

    @BuildStep
    public List<KubernetesAnnotationBuildItem> createAnnotations(KubernetesConfig kubernetesConfig,
            OpenshiftConfig openshiftConfig, KnativeConfig knativeConfig,
            Optional<MetricsCapabilityBuildItem> metricsConfiguration, List<KubernetesPortBuildItem> kubernetesPorts) {
        List<KubernetesAnnotationBuildItem> result = new ArrayList<KubernetesAnnotationBuildItem>();
        addAnnotations(kubernetesConfig, KUBERNETES, metricsConfiguration, kubernetesPorts, result);
        addAnnotations(kubernetesConfig, MINIKUBE, metricsConfiguration, kubernetesPorts, result);
        addAnnotations(openshiftConfig, OPENSHIFT, metricsConfiguration, kubernetesPorts, result);
        addAnnotations(knativeConfig, KNATIVE, metricsConfiguration, kubernetesPorts, result);
        return result;
    }

    private void addAnnotations(PlatformConfiguration config, String target,
            Optional<MetricsCapabilityBuildItem> metricsConfigurationBuildItem,
            List<KubernetesPortBuildItem> kubernetesPorts,
            List<KubernetesAnnotationBuildItem> result) {
        for (Map.Entry<String, String> entry : config.getAnnotations().entrySet()) {
            result.add(new KubernetesAnnotationBuildItem(entry.getKey(), entry.getValue(), target));
        }
        if (metricsConfigurationBuildItem.isPresent() && !kubernetesPorts.isEmpty()) {
            String path = metricsConfigurationBuildItem.get().metricsEndpoint();
            if (path != null) {
                result.add(new KubernetesAnnotationBuildItem(Prometheus.SCRAPE, "true", target));
                result.add(new KubernetesAnnotationBuildItem(Prometheus.PATH, path, target));
                result.add(new KubernetesAnnotationBuildItem(Prometheus.PORT, "" + kubernetesPorts.get(0).getPort(), target));
            }
        }
    }

    @BuildStep
    public void createLabels(KubernetesConfig kubernetesConfig, OpenshiftConfig openshiftConfig,
            KnativeConfig knativeConfig,
            BuildProducer<KubernetesLabelBuildItem> kubernetesLabelsProducer,
            BuildProducer<ContainerImageLabelBuildItem> containerImageLabelsProducer) {
        kubernetesConfig.labels.forEach((k, v) -> {
            kubernetesLabelsProducer.produce(new KubernetesLabelBuildItem(k, v, KUBERNETES));
            kubernetesLabelsProducer.produce(new KubernetesLabelBuildItem(k, v, MINIKUBE));
            containerImageLabelsProducer.produce(new ContainerImageLabelBuildItem(k, v));
        });
        openshiftConfig.labels.forEach((k, v) -> {
            kubernetesLabelsProducer.produce(new KubernetesLabelBuildItem(k, v, OPENSHIFT));
            containerImageLabelsProducer.produce(new ContainerImageLabelBuildItem(k, v));
        });
        knativeConfig.labels.forEach((k, v) -> {
            kubernetesLabelsProducer.produce(new KubernetesLabelBuildItem(k, v, KNATIVE));
            containerImageLabelsProducer.produce(new ContainerImageLabelBuildItem(k, v));
        });
    }

    @BuildStep
    public List<KubernetesEnvBuildItem> createEnv(KubernetesConfig kubernetesConfig, OpenshiftConfig openshiftConfig,
            KnativeConfig knativeConfig) {

        Collection<KubernetesEnvBuildItem> kubernetesEnvBuildItems = kubernetesConfig.convertToBuildItems();
        List<KubernetesEnvBuildItem> items = new ArrayList<>(kubernetesEnvBuildItems);
        for (KubernetesEnvBuildItem kubernetesEnvBuildItem : kubernetesEnvBuildItems) {
            items.add(kubernetesEnvBuildItem.newWithTarget(MINIKUBE));
        }
        items.addAll(openshiftConfig.convertToBuildItems());
        items.addAll(knativeConfig.convertToBuildItems());
        return items;
    }

    @BuildStep
    public void build(ApplicationInfoBuildItem applicationInfo,
            OutputTargetBuildItem outputTarget,
            PackageConfig packageConfig,
            KubernetesConfig kubernetesConfig,
            OpenshiftConfig openshiftConfig,
            KnativeConfig knativeConfig,
            Capabilities capabilities,
            LaunchModeBuildItem launchMode,
            List<KubernetesAnnotationBuildItem> kubernetesAnnotations,
            List<KubernetesLabelBuildItem> kubernetesLabels,
            List<KubernetesEnvBuildItem> kubernetesEnvs,
            List<KubernetesRoleBuildItem> kubernetesRoles,
            List<KubernetesRoleBindingBuildItem> kubernetesRoleBindings,
            List<KubernetesPortBuildItem> kubernetesPorts,
            EnabledKubernetesDeploymentTargetsBuildItem kubernetesDeploymentTargets,
            Optional<BaseImageInfoBuildItem> baseImage,
            Optional<ContainerImageInfoBuildItem> containerImage,
            Optional<KubernetesCommandBuildItem> command,
            Optional<KubernetesHealthLivenessPathBuildItem> kubernetesHealthLivenessPath,
            Optional<KubernetesHealthReadinessPathBuildItem> kubernetesHealthReadinessPath,
            BuildProducer<GeneratedFileSystemResourceBuildItem> generatedResourceProducer) {

        if (launchMode.getLaunchMode() == LaunchMode.TEST) {
            return;
        }

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

        Path artifactPath = outputTarget.getOutputDirectory().resolve(
                String.format(OUTPUT_ARTIFACT_FORMAT, outputTarget.getBaseName(), packageConfig.runnerSuffix));

        try {
            final Map<String, String> generatedResourcesMap;
            // by passing false to SimpleFileWriter, we ensure that no files are actually written during this phase
            Project project = createProject(applicationInfo, artifactPath);
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

            session.feed(Maps.fromProperties(config));

            //Apply configuration
            applyGlobalConfig(session, kubernetesConfig);

            ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

            boolean needToForceUpdateImagePullPolicy = needToForceUpdateImagePullPolicy(deploymentTargets, containerImage,
                    capabilities);
            applyConfig(session, project, KUBERNETES, getResourceName(kubernetesConfig, applicationInfo), kubernetesConfig,
                    now, determineImagePullPolicy(kubernetesConfig, needToForceUpdateImagePullPolicy));
            applyConfig(session, project, MINIKUBE, getResourceName(kubernetesConfig, applicationInfo), kubernetesConfig,
                    now, ImagePullPolicy.IfNotPresent);
            applyConfig(session, project, OPENSHIFT, getResourceName(openshiftConfig, applicationInfo), openshiftConfig, now,
                    determineImagePullPolicy(openshiftConfig, needToForceUpdateImagePullPolicy));
            applyConfig(session, project, KNATIVE, getResourceName(knativeConfig, applicationInfo), knativeConfig, now,
                    determineImagePullPolicy(knativeConfig, needToForceUpdateImagePullPolicy));

            applyKnativeConfig(session, project, getResourceName(knativeConfig, applicationInfo), knativeConfig);

            if (!capabilities.isCapabilityPresent(Capabilities.CONTAINER_IMAGE_S2I)) {
                handleNonS2IOpenshift(containerImage, session);
            }

            //apply build item configurations to the dekorate session.
            applyBuildItems(session,
                    applicationInfo,
                    kubernetesConfig,
                    openshiftConfig,
                    knativeConfig,
                    deploymentTargets,
                    kubernetesAnnotations,
                    kubernetesLabels,
                    kubernetesEnvs,
                    kubernetesRoles,
                    kubernetesRoleBindings,
                    kubernetesPorts,
                    baseImage,
                    containerImage,
                    command,
                    kubernetesHealthLivenessPath,
                    kubernetesHealthReadinessPath);

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
        } catch (Exception e) {
            if (launchMode.getLaunchMode() == LaunchMode.NORMAL) {
                throw e;
            }

            log.warn("Failed to generate Kubernetes resources", e);
        }
    }

    private void handleNonS2IOpenshift(Optional<ContainerImageInfoBuildItem> containerImage, Session session) {
        //When S2i is disabled we need to pass that information to dekorate.
        //Also we need to make sure that the alternatives (instances of ImageConfiguration)
        //are properly configured.
        session.configurators().add(new Configurator<ImageConfigurationFluent<?>>() {
            @Override
            public void visit(ImageConfigurationFluent<?> image) {
                containerImage.ifPresent(i -> {
                    String group = ImageUtil.getRepository(i.getImage()).split("/")[0];
                    image.withGroup(group);
                    i.getRegistry().ifPresent(r -> {
                        image.withRegistry(r);
                    });
                });
            }
        });

        //JAVA_APP_JAR value is not compatible with our Dockerfiles, so its causing problems
        session.resources().decorate(OPENSHIFT, new RemoveEnvVarDecorator("JAVA_APP_JAR"));
        session.configurators().add(new Configurator<S2iBuildConfigFluent<?>>() {
            @Override
            public void visit(S2iBuildConfigFluent<?> s2i) {
                s2i.withEnabled(false);
            }
        });

        // remove the ImageChange trigger of the DeploymentConfig
        session.resources().decorate(OPENSHIFT, new RemoveDeploymentTriggerDecorator());
    }

    /**
     * Apply global changes
     *
     * @param session The session to apply the changes
     * @param config The {@link KubernetesConfig} instance
     */
    private void applyGlobalConfig(Session session, KubernetesConfig config) {
        //Ports
        config.ports.entrySet().forEach(e -> session.configurators().add(new AddPort(PortConverter.convert(e))));
    }

    /**
     * Apply changes to the target resource group
     *
     * @param session The session to apply the changes
     * @param target The deployment target (e.g. kubernetes, openshift, knative)
     * @param name The name of the resource to accept the configuration
     * @param config The {@link PlatformConfiguration} instance
     * @param now ZonedDateTime indicating the current time
     * @param imagePullPolicy Kubernetes ImagePullPolicy to be used
     */
    private void applyConfig(Session session, Project project, String target, String name, PlatformConfiguration config,
            ZonedDateTime now, ImagePullPolicy imagePullPolicy) {
        if (OPENSHIFT.equals(target)) {
            session.resources().decorate(OPENSHIFT, new AddLabelDecorator(new Label(OPENSHIFT_APP_RUNTIME, QUARKUS)));
        }

        if (config.getNamespace().isPresent()) {
            session.resources().decorate(target, new AddNamespaceDecorator(config.getNamespace().get()));
        }

        applyAnnotations(session, project, target, config, now);

        config.getWorkingDir().ifPresent(w -> {
            session.resources().decorate(target, new ApplyWorkingDirDecorator(name, w));
        });

        config.getCommand().ifPresent(c -> {
            session.resources().decorate(target, new ApplyCommandDecorator(name, c.toArray(new String[0])));
        });

        config.getArguments().ifPresent(a -> {
            session.resources().decorate(target, new ApplyArgsDecorator(name, a.toArray(new String[0])));
        });

        config.getServiceAccount().ifPresent(s -> {
            session.resources().decorate(target, new ApplyServiceAccountNamedDecorator(name, s));
        });

        //Image Pull
        session.resources().decorate(target, new ApplyImagePullPolicyDecorator(imagePullPolicy));
        config.getImagePullSecrets().ifPresent(l -> {
            l.forEach(s -> session.resources().decorate(target, new AddImagePullSecretDecorator(name, s)));
        });

        // Mounts and Volumes
        config.getMounts().entrySet().forEach(e -> {
            session.resources().decorate(target, new AddMountDecorator(MountConverter.convert(e)));
        });

        config.getSecretVolumes().entrySet().forEach(e -> {
            session.resources().decorate(target, new AddSecretVolumeDecorator(SecretVolumeConverter.convert(e)));
        });

        config.getConfigMapVolumes().entrySet().forEach(e -> {
            session.resources().decorate(target, new AddConfigMapVolumeDecorator(ConfigMapVolumeConverter.convert(e)));
        });

        config.getPvcVolumes().entrySet().forEach(e -> {
            session.resources().decorate(target, new AddPvcVolumeDecorator(PvcVolumeConverter.convert(e)));
        });

        config.getAwsElasticBlockStoreVolumes().entrySet().forEach(e -> {
            session.resources().decorate(target,
                    new AddAwsElasticBlockStoreVolumeDecorator(AwsElasticBlockStoreVolumeConverter.convert(e)));
        });

        config.getAzureFileVolumes().entrySet().forEach(e -> {
            session.resources().decorate(target, new AddAzureFileVolumeDecorator(AzureFileVolumeConverter.convert(e)));
        });

        config.getAzureDiskVolumes().entrySet().forEach(e -> {
            session.resources().decorate(target, new AddAzureDiskVolumeDecorator(AzureDiskVolumeConverter.convert(e)));
        });

        config.getInitContainers().entrySet().forEach(e -> {
            session.resources().decorate(target, new AddInitContainerDecorator(name, ContainerConverter.convert(e)));
        });

        config.getSidecars().entrySet().forEach(e -> {
            session.resources().decorate(target, new AddSidecarDecorator(name, ContainerConverter.convert(e)));
        });

        // The presence of optional is causing issues in OCP 3.11, so we better remove them.
        // The following 4 decorator will set the optional property to null, so that it won't make it into the file.
        session.resources().decorate(target, new RemoveOptionalFromSecretEnvSourceDecorator());
        session.resources().decorate(target, new RemoveOptionalFromConfigMapEnvSourceDecorator());
        session.resources().decorate(target, new RemoveOptionalFromSecretKeySelectorDecorator());
        session.resources().decorate(target, new RemoveOptionalFromConfigMapKeySelectorDecorator());
    }

    private void applyAnnotations(Session session, Project project, String target, PlatformConfiguration config,
            ZonedDateTime now) {
        ScmInfo scm = project.getScmInfo();
        String vcsUrl = scm != null ? scm.getUrl() : null;
        String commitId = scm != null ? scm.getCommit() : null;

        //Dekorate uses its own annotations. Let's replace them with the quarkus ones.
        session.resources().decorate(target, new RemoveAnnotationDecorator(Annotations.VCS_URL));
        session.resources().decorate(target, new RemoveAnnotationDecorator(Annotations.COMMIT_ID));
        //Add quarkus vcs annotations
        if (commitId != null) {
            session.resources().decorate(target,
                    new AddAnnotationDecorator(new Annotation(QUARKUS_ANNOTATIONS_COMMIT_ID, commitId)));
        }
        if (vcsUrl != null) {
            session.resources().decorate(target,
                    new AddAnnotationDecorator(new Annotation(QUARKUS_ANNOTATIONS_VCS_URL, vcsUrl)));
        }

        if (config.isAddBuildTimestamp()) {
            session.resources().decorate(target, new AddAnnotationDecorator(new Annotation(QUARKUS_ANNOTATIONS_BUILD_TIMESTAMP,
                    now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd - HH:mm:ss Z")))));
        }
    }

    private void applyKnativeConfig(Session session, Project project, String name, KnativeConfig config) {
        if (config.clusterLocal) {
            session.resources().decorate(KNATIVE, new AddLabelDecorator(name,
                    new LabelBuilder()
                            .withKey("serving.knative.dev/visibility")
                            .withValue("cluster-local")
                            .build()));
        }

        config.minScale.ifPresent(min -> session.resources().decorate(KNATIVE, new ApplyMinScaleDecorator(name, min)));

        config.maxScale.ifPresent(max -> session.resources().decorate(KNATIVE, new ApplyMaxScaleDecorator(name, max)));

        config.revisionAutoScaling.autoScalerClass.map(AutoScalerClassConverter::convert)
                .ifPresent(a -> session.resources().decorate(KNATIVE, new ApplyLocalAutoscalingClassDecorator(name, a)));

        config.revisionAutoScaling.metric.map(AutoScalingMetricConverter::convert)
                .ifPresent(m -> session.resources().decorate(KNATIVE, new ApplyLocalAutoscalingMetricDecorator(name, m)));

        config.revisionAutoScaling.containerConcurrency
                .ifPresent(c -> session.resources().decorate(KNATIVE, new ApplyLocalContainerConcurrencyDecorator(name, c)));

        config.revisionAutoScaling.targetUtilizationPercentage
                .ifPresent(t -> session.resources().decorate(KNATIVE,
                        new ApplyLocalTargetUtilizationPercentageDecorator(name, t)));
        config.revisionAutoScaling.target
                .ifPresent(t -> session.resources().decorate(KNATIVE, new ApplyLocalAutoscalingTargetDecorator(name, t)));

        config.globalAutoScaling.autoScalerClass
                .map(AutoScalerClassConverter::convert)
                .ifPresent(a -> {
                    session.resources().decorate(new AddConfigMapResourceProvidingDecorator("config-autoscaler"));
                    session.resources().decorate(new ApplyGlobalAutoscalingClassDecorator(a));
                });

        config.globalAutoScaling.containerConcurrency
                .ifPresent(c -> {
                    session.resources().decorate(new AddConfigMapResourceProvidingDecorator("config-defaults"));
                    session.resources().decorate(new ApplyGlobalContainerConcurrencyDecorator(c));
                });

        config.globalAutoScaling.requestsPerSecond
                .ifPresent(r -> {
                    session.resources().decorate(new AddConfigMapResourceProvidingDecorator("config-autoscaler"));
                    session.resources().decorate(new ApplyGlobalRequestsPerSecondTargetDecorator(r));
                });

        config.globalAutoScaling.targetUtilizationPercentage
                .ifPresent(t -> {
                    session.resources().decorate(new AddConfigMapResourceProvidingDecorator("config-autoscaler"));
                    session.resources().decorate(new ApplyGlobalTargetUtilizationDecorator(t));
                });

        if (!config.scaleToZeroEnabled) {
            session.resources().decorate(new AddConfigMapResourceProvidingDecorator("config-autoscaler"));
            session.resources().decorate(new AddConfigMapDataDecorator("config-autoscaler", "enable-scale-to-zero",
                    String.valueOf(config.scaleToZeroEnabled)));
        }
    }

    /**
     * When there is no registry defined and s2i isn't being used, the only ImagePullPolicy that can work is 'IfNotPresent'.
     * This case comes up when users want to deploy their application to a cluster like Minikube where no registry is used
     * and instead they rely on the image being built directly into the docker daemon that the cluster uses.
     */
    private boolean needToForceUpdateImagePullPolicy(Collection<String> deploymentTargets,
            Optional<ContainerImageInfoBuildItem> containerImage,
            Capabilities capabilities) {

        // no need to change when we use Minikube only
        if ((deploymentTargets.size() == 1) && deploymentTargets.contains(MINIKUBE)) {
            return false;
        }

        boolean result = containerImage.isPresent()
                && ContainerImageUtil.isRegistryMissingAndNotS2I(capabilities, containerImage.get());
        if (result) {
            log.warn("No registry was set for the container image, so 'ImagePullPolicy' is being force-set to 'IfNotPresent'.");
            return true;
        }
        return false;
    }

    private ImagePullPolicy determineImagePullPolicy(PlatformConfiguration config, boolean needToForceUpdateImagePullPolicy) {
        if (needToForceUpdateImagePullPolicy) {
            return ImagePullPolicy.IfNotPresent;
        }
        return config.getImagePullPolicy();
    }

    private void applyBuildItems(Session session,
            ApplicationInfoBuildItem applicationInfo,
            KubernetesConfig kubernetesConfig,
            OpenshiftConfig openshiftConfig,
            KnativeConfig knativeConfig,
            Set<String> deploymentTargets,
            List<KubernetesAnnotationBuildItem> kubernetesAnnotations,
            List<KubernetesLabelBuildItem> kubernetesLabels,
            List<KubernetesEnvBuildItem> kubernetesEnvs,
            List<KubernetesRoleBuildItem> kubernetesRoles,
            List<KubernetesRoleBindingBuildItem> kubernetesRoleBindings,
            List<KubernetesPortBuildItem> kubernetesPorts,
            Optional<BaseImageInfoBuildItem> baseImage,
            Optional<ContainerImageInfoBuildItem> containerImage,
            Optional<KubernetesCommandBuildItem> command,
            Optional<KubernetesHealthLivenessPathBuildItem> kubernetesHealthLivenessPath,
            Optional<KubernetesHealthReadinessPathBuildItem> kubernetesHealthReadinessPath) {

        String kubernetesName = getResourceName(kubernetesConfig, applicationInfo);
        String openshiftName = getResourceName(openshiftConfig, applicationInfo);
        String knativeName = getResourceName(knativeConfig, applicationInfo);

        Map<String, PlatformConfiguration> configMap = new HashMap<>();
        configMap.put(KUBERNETES, kubernetesConfig);
        configMap.put(MINIKUBE, kubernetesConfig);
        configMap.put(OPENSHIFT, openshiftConfig);
        configMap.put(KNATIVE, knativeConfig);

        //Replicas
        if (kubernetesConfig.getReplicas() != 1) {
            session.resources().decorate(new io.dekorate.kubernetes.decorator.ApplyReplicasDecorator(kubernetesName,
                    kubernetesConfig.getReplicas()));
        }
        if (openshiftConfig.getReplicas() != 1) {
            session.resources()
                    .decorate(new io.dekorate.openshift.decorator.ApplyReplicasDecorator(openshiftName,
                            openshiftConfig.getReplicas()));
        }

        kubernetesAnnotations.forEach(a -> {
            session.resources().decorate(a.getTarget(), new AddAnnotationDecorator(new Annotation(a.getKey(), a.getValue())));
        });

        kubernetesLabels.forEach(l -> {
            session.resources().decorate(l.getTarget(), new AddLabelDecorator(new Label(l.getKey(), l.getValue())));
        });

        containerImage.ifPresent(c -> {
            session.resources().decorate(OPENSHIFT, new ApplyContainerImageDecorator(openshiftName, c.getImage()));
            session.resources().decorate(KUBERNETES, new ApplyContainerImageDecorator(kubernetesName, c.getImage()));
            session.resources().decorate(MINIKUBE, new ApplyContainerImageDecorator(kubernetesName, c.getImage()));
            session.resources().decorate(KNATIVE, new ApplyContainerImageDecorator(knativeName, c.getImage()));
        });

        kubernetesEnvs.forEach(e -> {
            String containerName = kubernetesName;
            if (e.getTarget().equals(OPENSHIFT)) {
                containerName = openshiftName;
            } else if (e.getTarget().equals(KNATIVE)) {
                containerName = knativeName;
            }
            session.resources().decorate(e.getTarget(), createAddEnvDecorator(e, containerName));
        });

        //Handle Command and arguments
        command.ifPresent(c -> {
            session.resources().decorate(new ApplyCommandDecorator(kubernetesName, new String[] { c.getCommand() }));
            session.resources().decorate(KUBERNETES, new ApplyArgsDecorator(kubernetesName, c.getArgs()));
            session.resources().decorate(MINIKUBE, new ApplyArgsDecorator(kubernetesName, c.getArgs()));

            session.resources().decorate(new ApplyCommandDecorator(openshiftName, new String[] { c.getCommand() }));
            session.resources().decorate(OPENSHIFT, new ApplyArgsDecorator(openshiftName, c.getArgs()));

            session.resources().decorate(new ApplyCommandDecorator(knativeName, new String[] { c.getCommand() }));
            session.resources().decorate(KNATIVE, new ApplyArgsDecorator(knativeName, c.getArgs()));
        });

        //Handle ports
        final Map<String, Integer> ports = verifyPorts(kubernetesPorts);
        ports.entrySet().stream()
                .map(e -> new PortBuilder().withName(e.getKey()).withContainerPort(e.getValue()).build())
                .forEach(p -> session.configurators().add(new AddPort(p)));

        //Handle RBAC
        // TODO why this condition?
        if (!kubernetesPorts.isEmpty()) {
            session.resources().decorate(new ApplyServiceAccountNamedDecorator());
            session.resources().decorate(new AddServiceAccountResourceDecorator());
            kubernetesRoles.forEach(r -> session.resources().decorate(new AddRoleResourceDecorator(r)));
            kubernetesRoleBindings.forEach(rb -> session.resources().decorate(
                    new AddRoleBindingResourceDecorator(rb.getName(), null, rb.getRole(),
                            rb.isClusterWide()
                                    ? AddRoleBindingResourceDecorator.RoleKind.ClusterRole
                                    : AddRoleBindingResourceDecorator.RoleKind.Role)));
        }

        handleServices(session, kubernetesConfig, openshiftConfig, knativeConfig, kubernetesName, openshiftName, knativeName);

        //Handle custom s2i builder images
        if (deploymentTargets.contains(OPENSHIFT)) {
            baseImage.map(BaseImageInfoBuildItem::getImage).ifPresent(builderImage -> {
                String builderImageName = ImageUtil.getName(builderImage);
                S2iBuildConfig s2iBuildConfig = new S2iBuildConfigBuilder().withBuilderImage(builderImage).build();
                if (!DEFAULT_S2I_IMAGE_NAME.equals(builderImageName)) {
                    session.resources().decorate(OPENSHIFT, new RemoveBuilderImageResourceDecorator(DEFAULT_S2I_IMAGE_NAME));
                }
                session.resources().decorate(OPENSHIFT, new AddBuilderImageStreamResourceDecorator(s2iBuildConfig));
                session.resources().decorate(OPENSHIFT, new ApplyBuilderImageDecorator(openshiftName, builderImage));
            });
        }

        // only use the probe config
        handleProbes(applicationInfo, kubernetesConfig, openshiftConfig, knativeConfig, deploymentTargets, ports,
                kubernetesHealthLivenessPath,
                kubernetesHealthReadinessPath, session);
    }

    private AddEnvVarDecorator createAddEnvDecorator(KubernetesEnvBuildItem e, String containerName) {
        return new AddEnvVarDecorator(ApplicationContainerDecorator.ANY, containerName, new EnvBuilder()
                .withName(EnvConverter.convertName(e.getName()))
                .withValue(e.getValue())
                .withSecret(e.getSecret())
                .withConfigmap(e.getConfigMap())
                .withField(e.getField())
                .build());
    }

    private void handleServices(Session session, KubernetesConfig kubernetesConfig, OpenshiftConfig openshiftConfig,
            KnativeConfig knativeConfig, String kubernetesName, String openshiftName, String knativeName) {
        session.resources().decorate(KUBERNETES,
                new ApplyServiceTypeDecorator(kubernetesName, kubernetesConfig.getServiceType().name()));
        if ((kubernetesConfig.getServiceType() == ServiceType.NodePort) && kubernetesConfig.nodePort.isPresent()) {
            session.resources().decorate(KUBERNETES,
                    new AddNodePortDecorator(openshiftName, kubernetesConfig.nodePort.getAsInt()));
        }
        session.resources().decorate(MINIKUBE,
                new ApplyServiceTypeDecorator(kubernetesName, ServiceType.NodePort.name()));
        session.resources().decorate(MINIKUBE, new AddNodePortDecorator(kubernetesName, kubernetesConfig.nodePort
                .orElseGet(() -> getStablePortNumberInRange(kubernetesName, MIN_NODE_PORT_VALUE, MAX_NODE_PORT_VALUE))));

        session.resources().decorate(OPENSHIFT,
                new ApplyServiceTypeDecorator(openshiftName, openshiftConfig.getServiceType().name()));
        if ((openshiftConfig.getServiceType() == ServiceType.NodePort) && openshiftConfig.nodePort.isPresent()) {
            session.resources().decorate(OPENSHIFT,
                    new AddNodePortDecorator(openshiftName, openshiftConfig.nodePort.getAsInt()));
        }

        session.resources().decorate(KNATIVE,
                new ApplyServiceTypeDecorator(knativeName, knativeConfig.getServiceType().name()));
    }

    /**
     * Given a string, generate a port number within the supplied range
     * The output is always the same (between {@code min} and {@code max})
     * given the same input and it's useful when we need to generate a port number
     * which needs to stay the same but we don't care about the exact value
     */
    private int getStablePortNumberInRange(String input, int min, int max) {
        if (min < MIN_PORT_NUMBER || max > MAX_PORT_NUMBER) {
            throw new IllegalArgumentException(
                    String.format("Port number range must be within [%d-%d]", MIN_PORT_NUMBER, MAX_PORT_NUMBER));
        }

        try {
            byte[] hash = MessageDigest.getInstance(DEFAULT_HASH_ALGORITHM).digest(input.getBytes(StandardCharsets.UTF_8));
            return min + new BigInteger(hash).mod(BigInteger.valueOf(max - min)).intValue();
        } catch (Exception e) {
            throw new RuntimeException("Unable to generate stable port number from input string: '" + input + "'", e);
        }
    }

    private void handleProbes(ApplicationInfoBuildItem applicationInfo, KubernetesConfig kubernetesConfig,
            OpenshiftConfig openshiftConfig,
            KnativeConfig knativeConfig,
            Set<String> deploymentTargets, Map<String, Integer> ports,
            Optional<KubernetesHealthLivenessPathBuildItem> kubernetesHealthLivenessPathBuildItem,
            Optional<KubernetesHealthReadinessPathBuildItem> kubernetesHealthReadinessPathBuildItem,
            Session session) {
        if (deploymentTargets.contains(KUBERNETES)) {
            doHandleProbes(getResourceName(kubernetesConfig, applicationInfo), KUBERNETES, ports,
                    kubernetesConfig.livenessProbe, kubernetesConfig.readinessProbe, kubernetesHealthLivenessPathBuildItem,
                    kubernetesHealthReadinessPathBuildItem, session);
        }
        if (deploymentTargets.contains(MINIKUBE)) {
            doHandleProbes(getResourceName(kubernetesConfig, applicationInfo), MINIKUBE, ports,
                    kubernetesConfig.livenessProbe, kubernetesConfig.readinessProbe, kubernetesHealthLivenessPathBuildItem,
                    kubernetesHealthReadinessPathBuildItem, session);
        }
        if (deploymentTargets.contains(OPENSHIFT)) {
            doHandleProbes(getResourceName(kubernetesConfig, applicationInfo), OPENSHIFT, ports, openshiftConfig.livenessProbe,
                    openshiftConfig.readinessProbe, kubernetesHealthLivenessPathBuildItem,
                    kubernetesHealthReadinessPathBuildItem, session);
        }
        if (deploymentTargets.contains(KNATIVE)) {
            doHandleProbes(getResourceName(kubernetesConfig, applicationInfo), KNATIVE, ports, knativeConfig.livenessProbe,
                    knativeConfig.readinessProbe, kubernetesHealthLivenessPathBuildItem, kubernetesHealthReadinessPathBuildItem,
                    session);
        }
    }

    private void doHandleProbes(String name, String target, Map<String, Integer> ports, ProbeConfig livenessProbe,
            ProbeConfig readinessProbe, Optional<KubernetesHealthLivenessPathBuildItem> kubernetesHealthLivenessPathBuildItem,
            Optional<KubernetesHealthReadinessPathBuildItem> kubernetesHealthReadinessPathBuildItem, Session session) {
        handleLivenessProbe(name, target, livenessProbe, kubernetesHealthLivenessPathBuildItem, session);
        handleReadinessProbe(name, target, readinessProbe, kubernetesHealthReadinessPathBuildItem,
                session);

        //For knative we want the port to be null
        Integer port = KNATIVE.equals(target) ? null : ports.getOrDefault(HTTP_PORT, DEFAULT_HTTP_PORT);
        session.resources().decorate(target, new ApplyHttpGetActionPortDecorator(port));
    }

    private void handleLivenessProbe(String name, String target, ProbeConfig livenessProbe,
            Optional<KubernetesHealthLivenessPathBuildItem> kubernetesHealthLivenessPathBuildItem, Session session) {
        AddLivenessProbeDecorator livenessProbeDecorator = null;
        if (livenessProbe.hasUserSuppliedAction()) {
            livenessProbeDecorator = new AddLivenessProbeDecorator(name,
                    ProbeConverter.convert(livenessProbe));
        } else if (kubernetesHealthLivenessPathBuildItem.isPresent()) {
            livenessProbeDecorator = new AddLivenessProbeDecorator(name,
                    ProbeConverter.builder(livenessProbe)
                            .withHttpActionPath(kubernetesHealthLivenessPathBuildItem.get().getPath()).build());
        }
        if (livenessProbeDecorator != null) {
            session.resources().decorate(target, livenessProbeDecorator);
        }
    }

    private void handleReadinessProbe(String name, String target, ProbeConfig readinessProbe,
            Optional<KubernetesHealthReadinessPathBuildItem> healthReadinessPathBuildItem, Session session) {
        AddReadinessProbeDecorator readinessProbeDecorator = null;
        if (readinessProbe.hasUserSuppliedAction()) {
            readinessProbeDecorator = new AddReadinessProbeDecorator(name, ProbeConverter.convert(readinessProbe));
        } else if (healthReadinessPathBuildItem.isPresent()) {
            readinessProbeDecorator = new AddReadinessProbeDecorator(name, ProbeConverter.builder(readinessProbe)
                    .withHttpActionPath(healthReadinessPathBuildItem.get().getPath()).build());
        }
        if (readinessProbeDecorator != null) {
            session.resources().decorate(target, readinessProbeDecorator);
        }
    }

    private Map<String, Integer> verifyPorts(List<KubernetesPortBuildItem> kubernetesPortBuildItems) {
        final Map<String, Integer> result = new HashMap<>();
        final Set<Integer> usedPorts = new HashSet<>();
        for (KubernetesPortBuildItem entry : kubernetesPortBuildItems) {
            final String name = entry.getName();
            if (result.containsKey(name)) {
                throw new IllegalArgumentException(
                        "All Kubernetes ports must have unique names - " + name + "has been used multiple times");
            }
            final Integer port = entry.getPort();
            if (usedPorts.contains(port)) {
                throw new IllegalArgumentException(
                        "All Kubernetes ports must be unique - " + port + "has been used multiple times");
            }
            result.put(name, port);
            usedPorts.add(port);
        }
        return result;
    }

    private Project createProject(ApplicationInfoBuildItem app, Path artifactPath) {
        //Let dekorate create a Project instance and then override with what is found in ApplicationInfoBuildItem.
        Project project = FileProjectFactory.create(artifactPath.toFile());
        BuildInfo buildInfo = new BuildInfo(app.getName(), app.getVersion(),
                "jar", project.getBuildInfo().getBuildTool(),
                artifactPath.toAbsolutePath().toString(),
                artifactPath,
                project.getBuildInfo().getResourceDir());

        return new Project(project.getRoot(), buildInfo, project.getScmInfo());
    }

}
