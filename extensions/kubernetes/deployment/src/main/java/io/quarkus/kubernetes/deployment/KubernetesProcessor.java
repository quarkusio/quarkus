package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.*;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.dekorate.Session;
import io.dekorate.SessionWriter;
import io.dekorate.kubernetes.config.EnvBuilder;
import io.dekorate.kubernetes.config.PortBuilder;
import io.dekorate.kubernetes.config.ProbeBuilder;
import io.dekorate.kubernetes.configurator.AddPort;
import io.dekorate.kubernetes.decorator.AddEnvVarDecorator;
import io.dekorate.kubernetes.decorator.AddLivenessProbeDecorator;
import io.dekorate.kubernetes.decorator.AddReadinessProbeDecorator;
import io.dekorate.kubernetes.decorator.AddRoleBindingResourceDecorator;
import io.dekorate.kubernetes.decorator.AddServiceAccountResourceDecorator;
import io.dekorate.kubernetes.decorator.ApplyArgsDecorator;
import io.dekorate.kubernetes.decorator.ApplyCommandDecorator;
import io.dekorate.kubernetes.decorator.ApplyImageDecorator;
import io.dekorate.kubernetes.decorator.ApplyServiceAccountNamedDecorator;
import io.dekorate.processor.SimpleFileWriter;
import io.dekorate.project.BuildInfo;
import io.dekorate.project.FileProjectFactory;
import io.dekorate.project.Project;
import io.dekorate.s2i.config.S2iBuildConfig;
import io.dekorate.s2i.config.S2iBuildConfigBuilder;
import io.dekorate.s2i.decorator.AddBuilderImageStreamResourceDecorator;
import io.dekorate.utils.Maps;
import io.dekorate.utils.Strings;
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

    private static final String PROPERTY_PREFIX = "dekorate.";
    private static final Set<String> ALLOWED_GENERATORS = new HashSet(
            Arrays.asList("kubernetes", "openshift", "knative", "docker", "s2i"));
    private static final Set<String> IMAGE_GENERATORS = new HashSet(Arrays.asList("docker", "s2i"));

    private static final String DOCKER_REGISTRY_PROPERTY = PROPERTY_PREFIX + "docker.registry";
    private static final String APP_GROUP_PROPERTY = "app.group";

    private static final String OUTPUT_ARTIFACT_FORMAT = "%s%s.jar";

    @BuildStep(onlyIf = IsNormal.class)
    public void build(ApplicationInfoBuildItem applicationInfo,
            ArchiveRootBuildItem archiveRootBuildItem,
            OutputTargetBuildItem outputTargetBuildItem,
            PackageConfig packageConfig,
            List<KubernetesEnvVarBuildItem> kubernetesEnvBuildItems,
            List<KubernetesRoleBuildItem> kubernetesRoleBuildItems,
            List<KubernetesPortBuildItem> kubernetesPortBuildItems,
            Optional<BaseImageInfoBuildItem> baseImageBuildItem,
            Optional<ContainerImageInfoBuildItem> containerImageBuildItem,
            Optional<KubernetesCommandBuildItem> commandBuildItem,
            Optional<KubernetesHealthLivenessPathBuildItem> kubernetesHealthLivenessPathBuildItem,
            Optional<KubernetesHealthReadinessPathBuildItem> kubernetesHealthReadinessPathBuildItem,
            BuildProducer<GeneratedFileSystemResourceBuildItem> generatedResourceProducer,
            BuildProducer<FeatureBuildItem> featureProducer)
            throws UnsupportedEncodingException {

        if (kubernetesPortBuildItems.isEmpty()) {
            return;
        }

        // by passing false to SimpleFileWriter, we ensure that no files are actually written during this phase
        final Path root;
        try {
            root = Files.createTempDirectory("quarkus-kubernetes");
        } catch (IOException e) {
            throw new RuntimeException("Unable to setup environment for generating Kubernetes resources", e);
        }

        Config config = ConfigProvider.getConfig();
        List<String> deploymentTargets = Arrays
                .stream(config.getOptionalValue(DEPLOYMENT_TARGET, String.class)
                        .orElse(KUBERNETES).split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        Map<String, Object> configAsMap = StreamSupport.stream(config.getPropertyNames().spliterator(), false)
                .filter(k -> ALLOWED_GENERATORS.contains(generatorName(k)))
                .collect(Collectors.toMap(k -> PROPERTY_PREFIX + k, k -> config.getValue(k, String.class)));

        // this is a hack to get kubernetes.registry working because currently it's not supported as is in Dekorate
        Optional<String> dockerRegistry = IMAGE_GENERATORS.stream()
                .map(g -> config.getOptionalValue(g + ".registry", String.class))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();

        dockerRegistry.ifPresent(v -> System.setProperty(DOCKER_REGISTRY_PROPERTY, v));

        // this is a hack to work around Dekorate using the default group for some of the properties
        Optional<String> kubernetesGroup = ALLOWED_GENERATORS.stream()
                .map(g -> config.getOptionalValue(g + ".group", String.class))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
        kubernetesGroup.ifPresent(v -> System.setProperty(APP_GROUP_PROPERTY, v));

        Path artifactPath = archiveRootBuildItem.getArchiveRoot().resolve(
                String.format(OUTPUT_ARTIFACT_FORMAT, outputTargetBuildItem.getBaseName(), packageConfig.runnerSuffix));
        final Map<String, String> generatedResourcesMap;
        try {
            final SessionWriter sessionWriter = new SimpleFileWriter(root, false);
            Project project = createProject(applicationInfo, artifactPath);
            sessionWriter.setProject(project);
            final Session session = Session.getSession();
            session.setWriter(sessionWriter);

            session.feed(Maps.fromProperties(configAsMap));

            //apply build item configurations to the dekorate session.
            applyBuildItems(session,
                    deploymentTargets,
                    applicationInfo,
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

    private void applyBuildItems(Session session,
            List<String> deploymentTargets,
            ApplicationInfoBuildItem applicationInfo,
            List<KubernetesEnvVarBuildItem> kubernetesEnvBuildItems,
            List<KubernetesRoleBuildItem> kubernetesRoleBuildItems,
            List<KubernetesPortBuildItem> kubernetesPortBuildItems,
            Optional<BaseImageInfoBuildItem> baseImageBuildItem,
            Optional<ContainerImageInfoBuildItem> containerImageBuildItem,
            Optional<KubernetesCommandBuildItem> commandBuildItem,
            Optional<KubernetesHealthLivenessPathBuildItem> kubernetesHealthLivenessPathBuildItem,
            Optional<KubernetesHealthReadinessPathBuildItem> kubernetesHealthReadinessPathBuildItem) {

        containerImageBuildItem.ifPresent(c -> session.resources()
                .decorate(new ApplyImageDecorator(applicationInfo.getName(), c.getImage())));

        //Handle env variables
        kubernetesEnvBuildItems.forEach(e -> session.resources()
                .decorate(new AddEnvVarDecorator(new EnvBuilder().withName(e.getName()).withValue(e.getValue()).build())));

        //Handle Command and arguments
        commandBuildItem.ifPresent(c -> {
            session.resources().decorate(new ApplyCommandDecorator(applicationInfo.getName(), new String[] { c.getCommand() }));
            session.resources().decorate(new ApplyArgsDecorator(applicationInfo.getName(), c.getArgs()));
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
                session.resources().decorate(DeploymentTarget.OPENSHIFT.name().toLowerCase(),
                        new AddBuilderImageStreamResourceDecorator(s2iBuildConfig));

                session.resources().decorate(new ApplyBuilderImageDecorator(applicationInfo.getName(), builderImage));
            });
        }

        //Handle probes
        kubernetesHealthLivenessPathBuildItem
                .ifPresent(l -> session.resources()
                        .decorate(new AddLivenessProbeDecorator(applicationInfo.getName(), new ProbeBuilder()
                                .withHttpActionPath(l.getPath())
                                .build())));
        kubernetesHealthReadinessPathBuildItem
                .ifPresent(r -> session.resources()
                        .decorate(new AddReadinessProbeDecorator(applicationInfo.getName(), new ProbeBuilder()
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

    /**
     * Returns the name of the generators that can handle the specified key.
     *
     * @param key The key.
     * @return The generator name or null if the key format is unexpected.
     */
    private static String generatorName(String key) {
        if (Strings.isNullOrEmpty(key) || !key.contains(".")) {
            return null;
        }
        return key.substring(0, key.indexOf("."));
    }

}
