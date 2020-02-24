package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.DEPLOY;
import static io.quarkus.kubernetes.deployment.Constants.KNATIVE;
import static io.quarkus.kubernetes.deployment.Constants.KUBERNETES;
import static io.quarkus.kubernetes.deployment.Constants.OPENSHIFT;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.dekorate.Session;
import io.dekorate.SessionWriter;
import io.dekorate.kubernetes.config.Annotation;
import io.dekorate.kubernetes.config.EnvBuilder;
import io.dekorate.kubernetes.config.Label;
import io.dekorate.kubernetes.config.PortBuilder;
import io.dekorate.kubernetes.config.ProbeBuilder;
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
import io.dekorate.kubernetes.decorator.ApplyImageDecorator;
import io.dekorate.kubernetes.decorator.ApplyImagePullPolicyDecorator;
import io.dekorate.kubernetes.decorator.ApplyServiceAccountNamedDecorator;
import io.dekorate.kubernetes.decorator.ApplyWorkingDirDecorator;
import io.dekorate.processor.SimpleFileWriter;
import io.dekorate.project.BuildInfo;
import io.dekorate.project.FileProjectFactory;
import io.dekorate.project.Project;
import io.dekorate.s2i.config.S2iBuildConfig;
import io.dekorate.s2i.config.S2iBuildConfigBuilder;
import io.dekorate.s2i.decorator.AddBuilderImageStreamResourceDecorator;
import io.dekorate.utils.Maps;
import io.quarkus.container.spi.BaseImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.ArchiveRootBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedFileSystemResourceBuildItem;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.kubernetes.spi.KubernetesCommandBuildItem;
import io.quarkus.kubernetes.spi.KubernetesEnvVarBuildItem;
import io.quarkus.kubernetes.spi.KubernetesHealthLivenessPathBuildItem;
import io.quarkus.kubernetes.spi.KubernetesHealthReadinessPathBuildItem;
import io.quarkus.kubernetes.spi.KubernetesPortBuildItem;
import io.quarkus.kubernetes.spi.KubernetesRoleBuildItem;

class KubernetesProcessor {

    private static final Logger LOG = Logger.getLogger(KubernetesDeployer.class);

    private static final String PROPERTY_PREFIX = "dekorate.";
    private static final String DOCKER_REGISTRY_PROPERTY = PROPERTY_PREFIX + "docker.registry";
    private static final String APP_GROUP_PROPERTY = "app.group";
    private static final String OUTPUT_ARTIFACT_FORMAT = "%s%s.jar";

    @BuildStep(onlyIf = IsNormal.class)
    public void build(ApplicationInfoBuildItem applicationInfo,
            ArchiveRootBuildItem archiveRootBuildItem,
            OutputTargetBuildItem outputTargetBuildItem,
            PackageConfig packageConfig,
            KubernetesConfig kubernetesConfig,
            OpenshiftConfig openshiftConfig,
            KnativeConfig knativeConfig,
            List<KubernetesEnvVarBuildItem> kubernetesEnvBuildItems,
            List<KubernetesRoleBuildItem> kubernetesRoleBuildItems,
            List<KubernetesPortBuildItem> kubernetesPortBuildItems,
            Optional<BaseImageInfoBuildItem> baseImageBuildItem,
            Optional<ContainerImageInfoBuildItem> containerImageBuildItem,
            Optional<KubernetesCommandBuildItem> commandBuildItem,
            Optional<KubernetesHealthLivenessPathBuildItem> kubernetesHealthLivenessPathBuildItem,
            Optional<KubernetesHealthReadinessPathBuildItem> kubernetesHealthReadinessPathBuildItem,
            BuildProducer<GeneratedFileSystemResourceBuildItem> generatedResourceProducer,
            BuildProducer<FeatureBuildItem> featureProducer) {

        if (kubernetesPortBuildItems.isEmpty()) {
            LOG.debug("The service is not an HTTP service so no Kubernetes manifests will be generated");
            return;
        }

        final Path root;
        try {
            root = Files.createTempDirectory("quarkus-kubernetes");
        } catch (IOException e) {
            throw new RuntimeException("Unable to setup environment for generating Kubernetes resources", e);
        }

        Map<String, Object> config = KubernetesConfigUtil.toMap(kubernetesConfig, openshiftConfig, knativeConfig);
        Set<String> deploymentTargets = new HashSet<>();
        deploymentTargets.addAll(KubernetesConfigUtil.getDeploymentTargets(config));
        deploymentTargets.addAll(kubernetesConfig.deploymentTarget.stream().map(Enum::name).map(String::toLowerCase)
                .collect(Collectors.toList()));

        // this is a hack to get kubernetes.registry working because currently it's not supported as is in Dekorate
        Optional<String> dockerRegistry = KubernetesConfigUtil.getDockerRegistry(config);
        dockerRegistry.ifPresent(v -> System.setProperty(DOCKER_REGISTRY_PROPERTY, v));

        // this is a hack to work around Dekorate using the default group for some of the properties
        Optional<String> kubernetesGroup = KubernetesConfigUtil.getGroup(config);
        kubernetesGroup.ifPresent(v -> System.setProperty(APP_GROUP_PROPERTY, v));

        Path artifactPath = archiveRootBuildItem.getArchiveRoot().resolve(
                String.format(OUTPUT_ARTIFACT_FORMAT, outputTargetBuildItem.getBaseName(), packageConfig.runnerSuffix));
        final Map<String, String> generatedResourcesMap;
        try {
            // by passing false to SimpleFileWriter, we ensure that no files are actually written during this phase
            final SessionWriter sessionWriter = new SimpleFileWriter(root, false);
            Project project = createProject(applicationInfo, artifactPath);
            sessionWriter.setProject(project);
            final Session session = Session.getSession();
            session.setWriter(sessionWriter);

            session.feed(Maps.fromProperties(config));

            //Apply configuration
            applyGlobalConfig(session, kubernetesConfig);
            applyConfig(session, KUBERNETES, kubernetesConfig.name.orElse(applicationInfo.getName()), kubernetesConfig);
            applyConfig(session, OPENSHIFT, openshiftConfig.name.orElse(applicationInfo.getName()), openshiftConfig);
            applyConfig(session, KNATIVE, knativeConfig.name.orElse(applicationInfo.getName()), knativeConfig);

            //apply build item configurations to the dekorate session.
            applyBuildItems(session,
                    applicationInfo.getName(),
                    kubernetesConfig,
                    openshiftConfig,
                    knativeConfig,
                    deploymentTargets,
                    kubernetesEnvBuildItems,
                    kubernetesRoleBuildItems,
                    kubernetesPortBuildItems,
                    baseImageBuildItem,
                    containerImageBuildItem,
                    commandBuildItem,
                    kubernetesHealthLivenessPathBuildItem,
                    kubernetesHealthReadinessPathBuildItem);

            // write the generated resources to the filesystem
            generatedResourcesMap = session.close();
        } finally {
            // clear the hacky properties if set
            if (kubernetesGroup.isPresent()) {
                System.clearProperty(APP_GROUP_PROPERTY);
            }
            if (dockerRegistry.isPresent()) {
                System.clearProperty(DOCKER_REGISTRY_PROPERTY);
            }
        }

        for (Map.Entry<String, String> resourceEntry : generatedResourcesMap.entrySet()) {
            String fileName = resourceEntry.getKey().replace(root.toAbsolutePath().toString(), "");
            String relativePath = resourceEntry.getKey().replace(root.toAbsolutePath().toString(), "kubernetes");

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

        featureProducer.produce(new FeatureBuildItem(FeatureBuildItem.KUBERNETES));
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
     */
    private void applyConfig(Session session, String target, String name, PlatformConfiguration config) {
        //Labels
        config.getLabels().forEach((key, value) -> {
            session.resources().decorate(target, new AddLabelDecorator(new Label(key, value)));
        });

        //Annotations
        config.getAnnotations().forEach((key, value) -> {
            session.resources().decorate(target, new AddAnnotationDecorator(new Annotation(key, value)));
        });

        //EnvVars
        config.getEnvVars().entrySet().forEach(e -> {
            session.resources().decorate(target, new AddEnvVarDecorator(EnvConverter.convert(e)));
        });

        config.getWorkingDir().ifPresent(w -> {
            session.resources().decorate(target, new ApplyWorkingDirDecorator(name, DEPLOY));
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
        session.resources().decorate(new ApplyImagePullPolicyDecorator(config.getImagePullPolicy()));
        config.getImagePullSecrets().ifPresent(l -> {
            l.forEach(s -> session.resources().decorate(target, new AddImagePullSecretDecorator(name, s)));
        });

        //Probes
        config.getLivenessProbe().ifPresent(p -> {
            session.resources().decorate(target, new AddLivenessProbeDecorator(name, ProbeConverter.convert(p)));
        });

        config.getReadinessProbe().ifPresent(p -> {
            session.resources().decorate(target,
                    new AddReadinessProbeDecorator(name, ProbeConverter.convert(p)));
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
            String name,
            KubernetesConfig kubernetesConfig,
            OpenshiftConfig openshiftConfig,
            KnativeConfig knativeConfig,
            Set<String> deploymentTargets,
            List<KubernetesEnvVarBuildItem> kubernetesEnvBuildItems,
            List<KubernetesRoleBuildItem> kubernetesRoleBuildItems,
            List<KubernetesPortBuildItem> kubernetesPortBuildItems,
            Optional<BaseImageInfoBuildItem> baseImageBuildItem,
            Optional<ContainerImageInfoBuildItem> containerImageBuildItem,
            Optional<KubernetesCommandBuildItem> commandBuildItem,
            Optional<KubernetesHealthLivenessPathBuildItem> kubernetesHealthLivenessPathBuildItem,
            Optional<KubernetesHealthReadinessPathBuildItem> kubernetesHealthReadinessPathBuildItem) {

        String kubernetesName = kubernetesConfig.name.orElse(name);
        String openshiftName = openshiftConfig.name.orElse(name);
        String knativeName = knativeConfig.name.orElse(name);

        //TODO: This is needed until  https://github.com/dekorateio/dekorate/issues/456 is resolved.
        containerImageBuildItem
                .ifPresent(c -> session.resources().decorate(new ApplyImageDecorator(kubernetesName, c.getImage())));

        containerImageBuildItem.ifPresent(
                c -> session.resources().decorate(KUBERNETES, new ApplyImageDecorator(kubernetesName, c.getImage())));
        containerImageBuildItem
                .ifPresent(c -> session.resources().decorate(OPENSHIFT, new ApplyImageDecorator(openshiftName, c.getImage())));
        containerImageBuildItem
                .ifPresent(c -> session.resources().decorate(KNATIVE, new ApplyImageDecorator(knativeName, c.getImage())));

        //Handle env variables
        kubernetesEnvBuildItems.forEach(e -> session.resources()
                .decorate(new AddEnvVarDecorator(new EnvBuilder().withName(e.getName()).withValue(e.getValue()).build())));

        //Handle Command and arguments
        commandBuildItem.ifPresent(c -> {
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
        final Map<String, Integer> ports = verifyPorts(kubernetesPortBuildItems);
        ports.entrySet().stream()
                .map(e -> new PortBuilder().withName(e.getKey()).withContainerPort(e.getValue()).build())
                .forEach(p -> session.configurators().add(new AddPort(p)));

        //Handle RBAC
        if (!kubernetesPortBuildItems.isEmpty()) {
            session.resources().decorate(new ApplyServiceAccountNamedDecorator());
            session.resources().decorate(new AddServiceAccountResourceDecorator());
            kubernetesRoleBuildItems
                    .forEach(r -> session.resources().decorate(new AddRoleBindingResourceDecorator(r.getRole())));
        }

        //Hanlde custom s2i builder images
        if (deploymentTargets.contains(DeploymentTarget.OPENSHIFT.name().toLowerCase())) {
            baseImageBuildItem.map(BaseImageInfoBuildItem::getImage).ifPresent(builderImage -> {
                S2iBuildConfig s2iBuildConfig = new S2iBuildConfigBuilder().withBuilderImage(builderImage).build();
                session.resources().decorate(OPENSHIFT, new AddBuilderImageStreamResourceDecorator(s2iBuildConfig));
                session.resources().decorate(OPENSHIFT, new ApplyBuilderImageDecorator(openshiftName, builderImage));
            });
        }

        //Handle probes
        kubernetesHealthLivenessPathBuildItem
                .ifPresent(l -> session.resources()
                        .decorate(new AddLivenessProbeDecorator(name, new ProbeBuilder()
                                .withHttpActionPath(l.getPath())
                                .build())));
        kubernetesHealthReadinessPathBuildItem
                .ifPresent(r -> session.resources()
                        .decorate(new AddReadinessProbeDecorator(name, new ProbeBuilder()
                                .withHttpActionPath(r.getPath())
                                .build())));
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
