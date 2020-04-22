package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.DEFAULT_HTTP_PORT;
import static io.quarkus.kubernetes.deployment.Constants.DEFAULT_S2I_IMAGE_NAME;
import static io.quarkus.kubernetes.deployment.Constants.DEPLOYMENT;
import static io.quarkus.kubernetes.deployment.Constants.DEPLOYMENT_CONFIG;
import static io.quarkus.kubernetes.deployment.Constants.HTTP_PORT;
import static io.quarkus.kubernetes.deployment.Constants.KNATIVE;
import static io.quarkus.kubernetes.deployment.Constants.KUBERNETES;
import static io.quarkus.kubernetes.deployment.Constants.OPENSHIFT;
import static io.quarkus.kubernetes.deployment.Constants.OPENSHIFT_APP_RUNTIME;
import static io.quarkus.kubernetes.deployment.Constants.QUARKUS;
import static io.quarkus.kubernetes.deployment.Constants.QUARKUS_ANNOTATIONS_BUILD_TIMESTAMP;
import static io.quarkus.kubernetes.deployment.Constants.QUARKUS_ANNOTATIONS_COMMIT_ID;
import static io.quarkus.kubernetes.deployment.Constants.QUARKUS_ANNOTATIONS_VCS_URL;
import static io.quarkus.kubernetes.deployment.Constants.SERVICE;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.dekorate.Session;
import io.dekorate.SessionWriter;
import io.dekorate.kubernetes.config.Annotation;
import io.dekorate.kubernetes.config.EnvBuilder;
import io.dekorate.kubernetes.config.Label;
import io.dekorate.kubernetes.config.PortBuilder;
import io.dekorate.kubernetes.configurator.AddPort;
import io.dekorate.kubernetes.decorator.AddAnnotationDecorator;
import io.dekorate.kubernetes.decorator.AddAwsElasticBlockStoreVolumeDecorator;
import io.dekorate.kubernetes.decorator.AddAzureDiskVolumeDecorator;
import io.dekorate.kubernetes.decorator.AddAzureFileVolumeDecorator;
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
import io.dekorate.kubernetes.decorator.AddSidecarDecorator;
import io.dekorate.kubernetes.decorator.ApplyArgsDecorator;
import io.dekorate.kubernetes.decorator.ApplyCommandDecorator;
import io.dekorate.kubernetes.decorator.ApplyImagePullPolicyDecorator;
import io.dekorate.kubernetes.decorator.ApplyServiceAccountNamedDecorator;
import io.dekorate.kubernetes.decorator.ApplyWorkingDirDecorator;
import io.dekorate.kubernetes.decorator.RemoveAnnotationDecorator;
import io.dekorate.processor.SimpleFileWriter;
import io.dekorate.project.BuildInfo;
import io.dekorate.project.FileProjectFactory;
import io.dekorate.project.Project;
import io.dekorate.project.ScmInfo;
import io.dekorate.s2i.config.S2iBuildConfig;
import io.dekorate.s2i.config.S2iBuildConfigBuilder;
import io.dekorate.s2i.decorator.AddBuilderImageStreamResourceDecorator;
import io.dekorate.utils.Annotations;
import io.dekorate.utils.Maps;
import io.quarkus.container.image.deployment.util.ImageUtil;
import io.quarkus.container.spi.BaseImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImageLabelBuildItem;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedFileSystemResourceBuildItem;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.util.FileUtil;
import io.quarkus.kubernetes.spi.KubernetesAnnotationBuildItem;
import io.quarkus.kubernetes.spi.KubernetesCommandBuildItem;
import io.quarkus.kubernetes.spi.KubernetesDeploymentTargetBuildItem;
import io.quarkus.kubernetes.spi.KubernetesEnvBuildItem;
import io.quarkus.kubernetes.spi.KubernetesHealthLivenessPathBuildItem;
import io.quarkus.kubernetes.spi.KubernetesHealthReadinessPathBuildItem;
import io.quarkus.kubernetes.spi.KubernetesLabelBuildItem;
import io.quarkus.kubernetes.spi.KubernetesPortBuildItem;
import io.quarkus.kubernetes.spi.KubernetesRoleBuildItem;

class KubernetesProcessor {

    private static final Logger log = Logger.getLogger(KubernetesDeployer.class);

    private static final String OUTPUT_ARTIFACT_FORMAT = "%s%s.jar";

    @BuildStep
    FeatureBuildItem produceFeature() {
        return new FeatureBuildItem(FeatureBuildItem.KUBERNETES);
    }

    @BuildStep
    public void checkKubernetes(BuildProducer<KubernetesDeploymentTargetBuildItem> deploymentTargets) {
        if (KubernetesConfigUtil.getDeploymentTargets().contains(KUBERNETES)) {
            deploymentTargets.produce(new KubernetesDeploymentTargetBuildItem(KUBERNETES, DEPLOYMENT));
        }
    }

    @BuildStep
    public void checkOpenshift(BuildProducer<KubernetesDeploymentTargetBuildItem> deploymentTargets) {
        if (KubernetesConfigUtil.getDeploymentTargets().contains(OPENSHIFT)) {
            deploymentTargets.produce(new KubernetesDeploymentTargetBuildItem(OPENSHIFT, DEPLOYMENT_CONFIG));
        }
    }

    @BuildStep
    public void checkKnative(BuildProducer<KubernetesDeploymentTargetBuildItem> deploymentTargets) {
        if (KubernetesConfigUtil.getDeploymentTargets().contains(KNATIVE)) {
            deploymentTargets.produce(new KubernetesDeploymentTargetBuildItem(KNATIVE, SERVICE));
        }
    }

    @BuildStep
    public List<KubernetesAnnotationBuildItem> createAnnotations(KubernetesConfig kubernetesConfig,
            OpenshiftConfig openshiftConfig, KnativeConfig knativeConfig) {
        List<KubernetesAnnotationBuildItem> items = new ArrayList<KubernetesAnnotationBuildItem>();
        kubernetesConfig.annotations.forEach((k, v) -> items.add(new KubernetesAnnotationBuildItem(k, v, KUBERNETES)));
        openshiftConfig.annotations.forEach((k, v) -> items.add(new KubernetesAnnotationBuildItem(k, v, OPENSHIFT)));
        knativeConfig.annotations.forEach((k, v) -> items.add(new KubernetesAnnotationBuildItem(k, v, KNATIVE)));
        return items;
    }

    @BuildStep
    public void createLabels(KubernetesConfig kubernetesConfig, OpenshiftConfig openshiftConfig,
            KnativeConfig knativeConfig,
            BuildProducer<KubernetesLabelBuildItem> kubernetesLabelsProducer,
            BuildProducer<ContainerImageLabelBuildItem> containerImageLabelsProducer) {
        kubernetesConfig.labels.forEach((k, v) -> {
            kubernetesLabelsProducer.produce(new KubernetesLabelBuildItem(k, v, KUBERNETES));
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
    public List<KubernetesEnvBuildItem> createEnvVars(KubernetesConfig kubernetesConfig, OpenshiftConfig openshiftConfig,
            KnativeConfig knativeConfig) {
        List<KubernetesEnvBuildItem> items = new ArrayList<KubernetesEnvBuildItem>();
        kubernetesConfig.envVars.forEach((k, v) -> items.add(new KubernetesEnvBuildItem(k, v.value.orElse(null),
                v.secret.orElse(null), v.configmap.orElse(null), v.field.orElse(null), KUBERNETES)));
        openshiftConfig.envVars.forEach((k, v) -> items.add(new KubernetesEnvBuildItem(k, v.value.orElse(null),
                v.secret.orElse(null), v.configmap.orElse(null), v.field.orElse(null), OPENSHIFT)));
        knativeConfig.envVars.forEach((k, v) -> items.add(new KubernetesEnvBuildItem(k, v.value.orElse(null),
                v.secret.orElse(null), v.configmap.orElse(null), v.field.orElse(null), KNATIVE)));
        return items;
    }

    @BuildStep(onlyIf = IsNormal.class)
    public void build(ApplicationInfoBuildItem applicationInfo,
            OutputTargetBuildItem outputTarget,
            PackageConfig packageConfig,
            KubernetesConfig kubernetesConfig,
            OpenshiftConfig openshiftConfig,
            KnativeConfig knativeConfig,
            List<KubernetesAnnotationBuildItem> kubernetesAnnotations,
            List<KubernetesLabelBuildItem> kubernetesLabels,
            List<KubernetesEnvBuildItem> kubernetesEnvs,
            List<KubernetesRoleBuildItem> kubernetesRoles,
            List<KubernetesPortBuildItem> kubernetesPorts,
            List<KubernetesDeploymentTargetBuildItem> kubernetesDeploymentTargets,
            Optional<BaseImageInfoBuildItem> baseImage,
            Optional<ContainerImageInfoBuildItem> containerImage,
            Optional<KubernetesCommandBuildItem> command,
            Optional<KubernetesHealthLivenessPathBuildItem> kubernetesHealthLivenessPath,
            Optional<KubernetesHealthReadinessPathBuildItem> kubernetesHealthReadinessPath,
            BuildProducer<GeneratedFileSystemResourceBuildItem> generatedResourceProducer) {

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
        Set<String> deploymentTargets = kubernetesDeploymentTargets.stream()
                .map(KubernetesDeploymentTargetBuildItem::getName)
                .collect(Collectors.toSet());

        Path artifactPath = outputTarget.getOutputDirectory().resolve(
                String.format(OUTPUT_ARTIFACT_FORMAT, outputTarget.getBaseName(), packageConfig.runnerSuffix));

        final Map<String, String> generatedResourcesMap;
        // by passing false to SimpleFileWriter, we ensure that no files are actually written during this phase
        final SessionWriter sessionWriter = new SimpleFileWriter(root, false);
        Project project = createProject(applicationInfo, artifactPath);
        sessionWriter.setProject(project);
        final Session session = Session.getSession();
        session.setWriter(sessionWriter);

        session.feed(Maps.fromProperties(config));

        //Apply configuration
        applyGlobalConfig(session, kubernetesConfig);
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        applyConfig(session, project, KUBERNETES, getResourceName(kubernetesConfig, applicationInfo), kubernetesConfig,
                now);
        applyConfig(session, project, OPENSHIFT, getResourceName(openshiftConfig, applicationInfo), openshiftConfig, now);
        applyConfig(session, project, KNATIVE, getResourceName(knativeConfig, applicationInfo), knativeConfig, now);

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
                kubernetesPorts,
                baseImage,
                containerImage,
                command,
                kubernetesHealthLivenessPath,
                kubernetesHealthReadinessPath);

        // write the generated resources to the filesystem
        generatedResourcesMap = session.close();

        for (Map.Entry<String, String> resourceEntry : generatedResourcesMap.entrySet()) {
            String fileName = resourceEntry.getKey().replace(root.toAbsolutePath().toString(), "");
            String relativePath = resourceEntry.getKey().replace(root.toAbsolutePath().toString(), KUBERNETES);

            if (fileName.endsWith(".yml") || fileName.endsWith(".json")) {
                String target = fileName.substring(0, fileName.lastIndexOf("."));
                if (target.startsWith(File.separator)) {
                    target = target.substring(1);
                }

                if (!deploymentTargets.contains(target)) {
                    continue;
                }
            }

            generatedResourceProducer.produce(
                    new GeneratedFileSystemResourceBuildItem(
                            // we need to make sure we are only passing the relative path to the build item
                            relativePath,
                            resourceEntry.getValue().getBytes(StandardCharsets.UTF_8)));
        }

        try {
            if (root != null && root.toFile().exists()) {
                FileUtil.deleteDirectory(root);
            }
        } catch (IOException e) {
            log.debug("Unable to delete temporary directory " + root, e);
        }
    }

    private String getResourceName(PlatformConfiguration platformConfiguration, ApplicationInfoBuildItem applicationInfo) {
        return platformConfiguration.getName().orElse(applicationInfo.getName());
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
     * @param now
     */
    private void applyConfig(Session session, Project project, String target, String name, PlatformConfiguration config,
            ZonedDateTime now) {
        if (OPENSHIFT.equals(target)) {
            session.resources().decorate(OPENSHIFT, new AddLabelDecorator(new Label(OPENSHIFT_APP_RUNTIME, QUARKUS)));
        }

        ScmInfo scm = project.getScmInfo();
        String vcsUrl = scm != null ? scm.getUrl() : Annotations.UNKNOWN;
        String commitId = scm != null ? scm.getCommit() : Annotations.UNKNOWN;

        //Dekorate uses its own annotations. Let's replace them with the quarkus ones.
        session.resources().decorate(target, new RemoveAnnotationDecorator(Annotations.VCS_URL));
        session.resources().decorate(target, new RemoveAnnotationDecorator(Annotations.COMMIT_ID));
        //Add quarkus vcs annotations
        session.resources().decorate(target,
                new AddAnnotationDecorator(new Annotation(QUARKUS_ANNOTATIONS_COMMIT_ID, commitId)));
        session.resources().decorate(target, new AddAnnotationDecorator(new Annotation(QUARKUS_ANNOTATIONS_VCS_URL, vcsUrl)));

        if (config.isAddBuildTimestamp()) {
            session.resources().decorate(target, new AddAnnotationDecorator(new Annotation(QUARKUS_ANNOTATIONS_BUILD_TIMESTAMP,
                    now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd - HH:mm:ss Z")))));
        }

        config.getWorkingDir().ifPresent(w -> {
            session.resources().decorate(target, new ApplyWorkingDirDecorator(name, w));
        });

        config.getCommand().ifPresent(c -> {
            session.resources().decorate(target,
                    new ApplyCommandDecorator(name, c.toArray(new String[c.size()])));
        });

        config.getArguments().ifPresent(a -> {
            session.resources().decorate(target, new ApplyArgsDecorator(name, a.toArray(new String[a.size()])));
        });

        config.getServiceAccount().ifPresent(s -> {
            session.resources().decorate(target, new ApplyServiceAccountNamedDecorator(name, s));
        });

        //Image Pull
        session.resources().decorate(target, new ApplyImagePullPolicyDecorator(config.getImagePullPolicy()));
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

        config.getContainers().entrySet().forEach(e -> {
            session.resources().decorate(target, new AddSidecarDecorator(name, ContainerConverter.convert(e)));
        });
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
        configMap.put(OPENSHIFT, openshiftConfig);
        configMap.put(KNATIVE, knativeConfig);

        //Replicas
        session.resources().decorate(new KubernetesApplyReplicasDecorator(kubernetesName, kubernetesConfig.getReplicas()));
        session.resources().decorate(new OpenshiftApplyReplicasDecorator(openshiftConfig.getReplicas()));

        kubernetesAnnotations.forEach(a -> {
            session.resources().decorate(a.getTarget(), new AddAnnotationDecorator(new Annotation(a.getKey(), a.getValue())));
        });

        kubernetesLabels.forEach(l -> {
            session.resources().decorate(l.getTarget(), new AddLabelDecorator(new Label(l.getKey(), l.getValue())));
        });

        containerImage.ifPresent(c -> {
            session.resources().decorate(OPENSHIFT, new ApplyContainerImageDecorator(openshiftName, c.getImage()));
            session.resources().decorate(KUBERNETES, new ApplyContainerImageDecorator(kubernetesName, c.getImage()));
            session.resources().decorate(KNATIVE, new ApplyContainerImageDecorator(knativeName, c.getImage()));
        });

        kubernetesEnvs.forEach(e -> {
            session.resources().decorate(e.getTarget(), new AddEnvVarDecorator(new EnvBuilder()
                    .withName(e.getName() == null ? null : e.getName().toUpperCase().replaceAll(Pattern.quote("-"), "_"))
                    .withValue(e.getValue())
                    .withSecret(e.getSecret())
                    .withConfigmap(e.getConfigmap())
                    .withField(e.getField())
                    .build()));
        });

        //Handle Command and arguments
        command.ifPresent(c -> {
            session.resources()
                    .decorate(new ApplyCommandDecorator(kubernetesName, new String[] { c.getCommand() }));
            session.resources().decorate(KUBERNETES, new ApplyArgsDecorator(kubernetesName, c.getArgs()));

            session.resources()
                    .decorate(new ApplyCommandDecorator(openshiftName, new String[] { c.getCommand() }));
            session.resources().decorate(OPENSHIFT, new ApplyArgsDecorator(openshiftName, c.getArgs()));

            session.resources()
                    .decorate(new ApplyCommandDecorator(knativeName, new String[] { c.getCommand() }));
            session.resources().decorate(KNATIVE, new ApplyArgsDecorator(knativeName, c.getArgs()));
        });

        //Handle ports
        final Map<String, Integer> ports = verifyPorts(kubernetesPorts);
        ports.entrySet().stream()
                .map(e -> new PortBuilder().withName(e.getKey()).withContainerPort(e.getValue()).build())
                .forEach(p -> session.configurators().add(new AddPort(p)));

        //Handle RBAC
        if (!kubernetesPorts.isEmpty()) {
            session.resources().decorate(new ApplyServiceAccountNamedDecorator());
            session.resources().decorate(new AddServiceAccountResourceDecorator());
            kubernetesRoles
                    .forEach(r -> session.resources().decorate(new AddRoleBindingResourceDecorator(r.getRole())));
        }

        session.resources().decorate(KUBERNETES,
                new ApplyServiceTypeDecorator(kubernetesName, kubernetesConfig.getServiceType().name()));
        session.resources().decorate(OPENSHIFT,
                new ApplyServiceTypeDecorator(openshiftName, openshiftConfig.getServiceType().name()));
        session.resources().decorate(KNATIVE,
                new ApplyServiceTypeDecorator(knativeName, knativeConfig.getServiceType().name()));

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
        handleProbes(applicationInfo, kubernetesConfig, openshiftConfig, knativeConfig, ports, kubernetesHealthLivenessPath,
                kubernetesHealthReadinessPath, session);
    }

    private void handleProbes(ApplicationInfoBuildItem applicationInfo, KubernetesConfig kubernetesConfig,
            OpenshiftConfig openshiftConfig,
            KnativeConfig knativeConfig,
            Map<String, Integer> ports,
            Optional<KubernetesHealthLivenessPathBuildItem> kubernetesHealthLivenessPathBuildItem,
            Optional<KubernetesHealthReadinessPathBuildItem> kubernetesHealthReadinessPathBuildItem,
            Session session) {
        if (kubernetesConfig.deploymentTarget.contains(KUBERNETES)) {
            doHandleProbes(getResourceName(kubernetesConfig, applicationInfo), KUBERNETES, ports,
                    kubernetesConfig.livenessProbe, kubernetesConfig.readinessProbe, kubernetesHealthLivenessPathBuildItem,
                    kubernetesHealthReadinessPathBuildItem, session);
        }
        if (kubernetesConfig.deploymentTarget.contains(OPENSHIFT)) {
            doHandleProbes(getResourceName(kubernetesConfig, applicationInfo), OPENSHIFT, ports, openshiftConfig.livenessProbe,
                    openshiftConfig.readinessProbe, kubernetesHealthLivenessPathBuildItem,
                    kubernetesHealthReadinessPathBuildItem, session);
        }
        if (kubernetesConfig.deploymentTarget.contains(KNATIVE)) {
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
        session.resources().decorate(target,
                new ApplyHttpGetActionPortDecorator(ports.getOrDefault(HTTP_PORT, DEFAULT_HTTP_PORT)));
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
                artifactPath,
                project.getBuildInfo().getOutputFile(),
                project.getBuildInfo().getClassOutputDir());

        return new Project(project.getRoot(), buildInfo, project.getScmInfo());
    }

}
