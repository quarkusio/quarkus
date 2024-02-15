package io.quarkus.container.image.openshift.deployment;

import static io.quarkus.container.image.openshift.deployment.OpenshiftUtils.getDeployStrategy;
import static io.quarkus.container.image.openshift.deployment.OpenshiftUtils.getNamespace;
import static io.quarkus.container.image.openshift.deployment.OpenshiftUtils.mergeConfig;
import static io.quarkus.container.util.PathsUtil.findMainSourcesRoot;
import static io.quarkus.deployment.pkg.steps.JarResultBuildStep.DEFAULT_FAST_JAR_DIRECTORY_NAME;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.dekorate.kubernetes.decorator.AddDockerConfigJsonSecretDecorator;
import io.dekorate.utils.Packaging;
import io.dekorate.utils.Serialization;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import io.fabric8.kubernetes.client.dsl.NamespaceableResource;
import io.fabric8.kubernetes.client.dsl.base.PatchContext;
import io.fabric8.kubernetes.client.dsl.base.PatchType;
import io.fabric8.kubernetes.client.http.HttpClient;
import io.fabric8.openshift.api.model.Build;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.ImageStream;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.container.image.deployment.ContainerImageConfig;
import io.quarkus.container.image.deployment.util.ImageUtil;
import io.quarkus.container.spi.AvailableContainerImageExtensionBuildItem;
import io.quarkus.container.spi.BaseImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImageBuildRequestBuildItem;
import io.quarkus.container.spi.ContainerImageBuilderBuildItem;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImagePushRequestBuildItem;
import io.quarkus.deployment.IsNormalNotRemoteDev;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.ArchiveRootBuildItem;
import io.quarkus.deployment.builditem.GeneratedFileSystemResourceBuildItem;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.CompiledJavaVersionBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.builditem.JarBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.kubernetes.client.deployment.KubernetesClientErrorHandler;
import io.quarkus.kubernetes.client.spi.KubernetesClientBuildItem;
import io.quarkus.kubernetes.spi.DecoratorBuildItem;
import io.quarkus.kubernetes.spi.DeployStrategy;
import io.quarkus.kubernetes.spi.KubernetesCommandBuildItem;
import io.quarkus.kubernetes.spi.KubernetesEnvBuildItem;

public class OpenshiftProcessor {

    public static final String OPENSHIFT = "openshift";
    private static final String BUILD_CONFIG_NAME = "openshift.io/build-config.name";
    private static final String RUNNING = "Running";
    private static final String JAVA_APP_JAR = "JAVA_APP_JAR";
    private static final String OPENSHIFT_INTERNAL_REGISTRY = "openshift-image-registry";

    private static final int LOG_TAIL_SIZE = 10;
    private static final Logger LOG = Logger.getLogger(OpenshiftProcessor.class);

    @BuildStep
    public AvailableContainerImageExtensionBuildItem availability() {
        return new AvailableContainerImageExtensionBuildItem(OPENSHIFT);
    }

    @BuildStep(onlyIf = { OpenshiftBuild.class }, onlyIfNot = NativeBuild.class)
    public void openshiftPrepareJvmDockerBuild(ContainerImageOpenshiftConfig openshiftConfig,
            S2iConfig s2iConfig,
            OutputTargetBuildItem out,
            BuildProducer<DecoratorBuildItem> decorator) {
        ContainerImageOpenshiftConfig config = mergeConfig(openshiftConfig, s2iConfig);
        if (config.buildStrategy == BuildStrategy.DOCKER) {
            decorator.produce(new DecoratorBuildItem(new ApplyDockerfileToBuildConfigDecorator(null,
                    findMainSourcesRoot(out.getOutputDirectory()).getValue().resolve(openshiftConfig.jvmDockerfile))));
            //When using the docker build strategy, we can't possibly know these values, so it's the image responsibility to work without them.
            decorator.produce(new DecoratorBuildItem(new RemoveEnvVarDecorator(null, "JAVA_APP_JAR")));
            decorator.produce(new DecoratorBuildItem(new RemoveEnvVarDecorator(null, "JAVA_APP_LIB")));
        }
    }

    @BuildStep(onlyIf = { OpenshiftBuild.class, NativeBuild.class })
    public void openshiftPrepareNativeDockerBuild(ContainerImageOpenshiftConfig openshiftConfig,
            S2iConfig s2iConfig,
            OutputTargetBuildItem out,
            BuildProducer<DecoratorBuildItem> decorator) {
        ContainerImageOpenshiftConfig config = mergeConfig(openshiftConfig, s2iConfig);
        if (config.buildStrategy == BuildStrategy.DOCKER) {
            decorator.produce(new DecoratorBuildItem(new ApplyDockerfileToBuildConfigDecorator(null,
                    findMainSourcesRoot(out.getOutputDirectory()).getValue().resolve(openshiftConfig.nativeDockerfile))));
        }
        //Let's remove this for all kinds of native build
        decorator.produce(new DecoratorBuildItem(new RemoveEnvVarDecorator(null, "JAVA_APP_JAR")));
        decorator.produce(new DecoratorBuildItem(new RemoveEnvVarDecorator(null, "JAVA_APP_LIB")));
    }

    @BuildStep(onlyIf = { IsNormalNotRemoteDev.class, OpenshiftBuild.class }, onlyIfNot = NativeBuild.class)
    public void openshiftRequirementsJvm(ContainerImageOpenshiftConfig openshiftConfig,
            S2iConfig s2iConfig,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            OutputTargetBuildItem out,
            PackageConfig packageConfig,
            JarBuildItem jarBuildItem,
            CompiledJavaVersionBuildItem compiledJavaVersion,
            BuildProducer<DecoratorBuildItem> decorator,
            BuildProducer<KubernetesEnvBuildItem> envProducer,
            BuildProducer<BaseImageInfoBuildItem> builderImageProducer,
            BuildProducer<KubernetesCommandBuildItem> commandProducer) {

        ContainerImageOpenshiftConfig config = mergeConfig(openshiftConfig, s2iConfig);
        String outputJarFileName = jarBuildItem.getPath().getFileName().toString();
        String jarFileName = config.jarFileName.orElse(outputJarFileName);
        String baseJvmImage = config.baseJvmImage
                .orElse(ContainerImageOpenshiftConfig.getDefaultJvmImage(compiledJavaVersion.getJavaVersion()));

        boolean hasCustomJarPath = config.jarFileName.isPresent() || config.jarDirectory.isPresent();
        boolean hasCustomJvmArguments = config.jvmArguments.isPresent();

        builderImageProducer.produce(new BaseImageInfoBuildItem(baseJvmImage));

        if (config.buildStrategy == BuildStrategy.BINARY) {
            // Jar directory priorities:
            // 1. explicitly specified by the user.
            // 3. fallback value
            String jarDirectory = config.jarDirectory.orElse(config.FALLBACK_JAR_DIRECTORY);
            String pathToJar = concatUnixPaths(jarDirectory, jarFileName);

            //In all other cases its the responsibility of the image to set those up correctly.
            if (hasCustomJarPath || hasCustomJvmArguments) {
                List<String> cmd = new ArrayList<>();
                cmd.add("java");
                cmd.addAll(config.getEffectiveJvmArguments());
                cmd.addAll(Arrays.asList("-jar", pathToJar));
                envProducer.produce(KubernetesEnvBuildItem.createSimpleVar(JAVA_APP_JAR, pathToJar, null));
                commandProducer.produce(KubernetesCommandBuildItem.command(cmd));
            }
        }
    }

    @BuildStep(onlyIf = { IsNormalNotRemoteDev.class, OpenshiftBuild.class, NativeBuild.class })
    public void openshiftRequirementsNative(ContainerImageOpenshiftConfig openshiftConfig,
            S2iConfig s2iConfig,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            OutputTargetBuildItem out,
            PackageConfig packageConfig,
            NativeImageBuildItem nativeImage,
            BuildProducer<KubernetesEnvBuildItem> envProducer,
            BuildProducer<BaseImageInfoBuildItem> builderImageProducer,
            BuildProducer<KubernetesCommandBuildItem> commandProducer) {

        ContainerImageOpenshiftConfig config = mergeConfig(openshiftConfig, s2iConfig);
        boolean usingDefaultBuilder = ImageUtil.getRepository(ContainerImageOpenshiftConfig.DEFAULT_BASE_NATIVE_IMAGE)
                .equals(ImageUtil.getRepository(config.baseNativeImage));
        String outputNativeBinaryFileName = nativeImage.getPath().getFileName().toString();

        String nativeBinaryFileName = null;

        boolean hasCustomNativePath = config.nativeBinaryFileName.isPresent() || config.nativeBinaryDirectory.isPresent();
        boolean hasCustomNativeArguments = config.nativeArguments.isPresent();

        //The default openshift builder for native builds, renames the native binary.
        //To make things easier for the user, we need to handle it.
        if (usingDefaultBuilder && !config.nativeBinaryFileName.isPresent()) {
            nativeBinaryFileName = ContainerImageOpenshiftConfig.DEFAULT_NATIVE_TARGET_FILENAME;
        } else {
            nativeBinaryFileName = config.nativeBinaryFileName.orElse(outputNativeBinaryFileName);
        }

        if (config.buildStrategy == BuildStrategy.BINARY) {
            builderImageProducer.produce(new BaseImageInfoBuildItem(config.baseNativeImage));
            // Native binary directory priorities:
            // 1. explicitly specified by the user.
            // 2. fallback vale

            String nativeBinaryDirectory = config.nativeBinaryDirectory.orElse(config.FALLBACK_NATIVE_BINARY_DIRECTORY);
            String pathToNativeBinary = concatUnixPaths(nativeBinaryDirectory, nativeBinaryFileName);
            if (hasCustomNativePath || hasCustomNativeArguments) {
                commandProducer
                        .produce(KubernetesCommandBuildItem.commandWithArgs(pathToNativeBinary, config.nativeArguments.get()));
            }
        }
    }

    @BuildStep(onlyIf = { OpenshiftBuild.class })
    public void configureExternalRegistry(ApplicationInfoBuildItem applicationInfo,
            ContainerImageOpenshiftConfig openshiftConfig,
            ContainerImageInfoBuildItem containerImageInfo,
            BuildProducer<DecoratorBuildItem> decorator) {
        containerImageInfo.registry.ifPresent(registry -> {
            final String name = applicationInfo.getName();
            final String serviceAccountName = applicationInfo.getName();
            String repositoryWithRegistry = registry + "/" + containerImageInfo.getRepository();

            if (openshiftConfig.imagePushSecret.isPresent()) {
                //if a push secret has been specified, we need to apply it.
                String imagePushSecret = openshiftConfig.imagePushSecret.get();
                decorator.produce(new DecoratorBuildItem(OPENSHIFT, new ApplyDockerImageOutputToBuildConfigDecorator(
                        applicationInfo.getName(), containerImageInfo.getImage(), imagePushSecret)));
            } else if (registry.contains(OPENSHIFT_INTERNAL_REGISTRY)) {
                //no special handling of secrets is really needed.
            } else if (containerImageInfo.username.isPresent() && containerImageInfo.password.isPresent()) {
                String imagePushSecret = applicationInfo.getName() + "-push-secret";
                decorator.produce(new DecoratorBuildItem(OPENSHIFT,
                        new AddDockerConfigJsonSecretDecorator(imagePushSecret, containerImageInfo.registry.get(),
                                containerImageInfo.username.get(), containerImageInfo.password.get())));
                decorator.produce(new DecoratorBuildItem(OPENSHIFT, new ApplyDockerImageOutputToBuildConfigDecorator(
                        applicationInfo.getName(), containerImageInfo.getImage(), imagePushSecret)));
            } else {
                LOG.warn("An external image registry has been specified, but no push secret or credentials.");
            }

            decorator.produce(new DecoratorBuildItem(OPENSHIFT,
                    new ApplyDockerImageRepositoryToImageStream(applicationInfo.getName(), repositoryWithRegistry)));
        });
    }

    @BuildStep(onlyIf = { IsNormalNotRemoteDev.class, OpenshiftBuild.class }, onlyIfNot = NativeBuild.class)
    public void openshiftBuildFromJar(ContainerImageOpenshiftConfig openshiftConfig,
            S2iConfig s2iConfig,
            ContainerImageConfig containerImageConfig,
            KubernetesClientBuildItem kubernetesClientBuilder,
            ContainerImageInfoBuildItem containerImage,
            ArchiveRootBuildItem archiveRoot, OutputTargetBuildItem out, PackageConfig packageConfig,
            List<GeneratedFileSystemResourceBuildItem> generatedResources,
            Optional<ContainerImageBuildRequestBuildItem> buildRequest,
            Optional<ContainerImagePushRequestBuildItem> pushRequest,
            BuildProducer<ArtifactResultBuildItem> artifactResultProducer,
            BuildProducer<ContainerImageBuilderBuildItem> containerImageBuilder,
            // used to ensure that the jar has been built
            JarBuildItem jar) {

        ContainerImageOpenshiftConfig config = mergeConfig(openshiftConfig, s2iConfig);
        if (containerImageConfig.isBuildExplicitlyDisabled()) {
            return;
        }

        if (!containerImageConfig.isBuildExplicitlyEnabled() && !containerImageConfig.isPushExplicitlyEnabled()
                && !buildRequest.isPresent() && !pushRequest.isPresent()) {
            return;
        }

        Optional<GeneratedFileSystemResourceBuildItem> openshiftYml = generatedResources
                .stream()
                .filter(r -> r.getName().endsWith(File.separator + "openshift.yml"))
                .findFirst();

        if (openshiftYml.isEmpty()) {
            LOG.warn(
                    "No OpenShift manifests were generated so no OpenShift build process will be taking place");
            return;
        }

        try (KubernetesClient kubernetesClient = buildClient(kubernetesClientBuilder)) {
            String namespace = Optional.ofNullable(kubernetesClient.getNamespace()).orElse("default");
            LOG.info("Starting (in-cluster) container image build for jar using: " + config.buildStrategy + " on server: "
                    + kubernetesClient.getMasterUrl() + " in namespace:" + namespace + ".");
            //The contextRoot is where inside the tarball we will add the jars. A null value means everything will be added under '/' while "target" means everything will be added under '/target'.
            //For docker kind of builds where we use instructions like: `COPY target/*.jar /deployments` it using '/target' is a requirement.
            //For s2i kind of builds where jars are expected directly in the '/' we have to use null.
            String outputDirName = out.getOutputDirectory().getFileName().toString();
            String contextRoot = getContextRoot(outputDirName, packageConfig.isFastJar(), config.buildStrategy);
            KubernetesClientBuilder clientBuilder = newClientBuilderWithoutHttp2(kubernetesClient.getConfiguration(),
                    kubernetesClientBuilder.getHttpClientFactory());
            if (packageConfig.isFastJar()) {
                createContainerImage(clientBuilder, openshiftYml.get(), config, contextRoot, jar.getPath().getParent(),
                        jar.getPath().getParent());
            } else if (jar.getLibraryDir() != null) { //When using uber-jar the libraryDir is going to be null, potentially causing NPE.
                createContainerImage(clientBuilder, openshiftYml.get(), config, contextRoot, jar.getPath().getParent(),
                        jar.getPath(), jar.getLibraryDir());
            } else {
                createContainerImage(clientBuilder, openshiftYml.get(), config, contextRoot, jar.getPath().getParent(),
                        jar.getPath());
            }
            artifactResultProducer.produce(new ArtifactResultBuildItem(null, "jar-container", Collections.emptyMap()));
            containerImageBuilder.produce(new ContainerImageBuilderBuildItem(OPENSHIFT));
        }
    }

    private String getContextRoot(String outputDirName, boolean isFastJar, BuildStrategy buildStrategy) {
        if (buildStrategy != BuildStrategy.DOCKER) {
            return null;
        }
        if (!isFastJar) {
            return outputDirName;
        }
        return outputDirName + "/" + DEFAULT_FAST_JAR_DIRECTORY_NAME;
    }

    @BuildStep(onlyIf = { IsNormalNotRemoteDev.class, OpenshiftBuild.class, NativeBuild.class })
    public void openshiftBuildFromNative(ContainerImageOpenshiftConfig openshiftConfig, S2iConfig s2iConfig,
            ContainerImageConfig containerImageConfig,
            KubernetesClientBuildItem kubernetesClientBuilder,
            ContainerImageInfoBuildItem containerImage,
            ArchiveRootBuildItem archiveRoot, OutputTargetBuildItem out, PackageConfig packageConfig,
            List<GeneratedFileSystemResourceBuildItem> generatedResources,
            Optional<ContainerImageBuildRequestBuildItem> buildRequest,
            Optional<ContainerImagePushRequestBuildItem> pushRequest,
            BuildProducer<ArtifactResultBuildItem> artifactResultProducer,
            BuildProducer<ContainerImageBuilderBuildItem> containerImageBuilder,
            NativeImageBuildItem nativeImage) {

        ContainerImageOpenshiftConfig config = mergeConfig(openshiftConfig, s2iConfig);

        if (containerImageConfig.isBuildExplicitlyDisabled()) {
            return;
        }

        if (!containerImageConfig.isBuildExplicitlyEnabled() && !containerImageConfig.isPushExplicitlyEnabled()
                && !buildRequest.isPresent() && !pushRequest.isPresent()) {
            return;
        }

        try (KubernetesClient kubernetesClient = buildClient(kubernetesClientBuilder)) {
            String namespace = Optional.ofNullable(kubernetesClient.getNamespace()).orElse("default");

            LOG.info("Starting (in-cluster) container image build for jar using: " + config.buildStrategy + " on server: "
                    + kubernetesClient.getMasterUrl() + " in namespace:" + namespace + ".");
            Optional<GeneratedFileSystemResourceBuildItem> openshiftYml = generatedResources
                    .stream()
                    .filter(r -> r.getName().endsWith(File.separator + "openshift.yml"))
                    .findFirst();

            if (openshiftYml.isEmpty()) {
                LOG.warn(
                        "No OpenShift manifests were generated so no OpenShift build process will be taking place");
                return;
            }
            //The contextRoot is where inside the tarball we will add the jars. A null value means everything will be added under '/' while "target" means everything will be added under '/target'.
            //For docker kind of builds where we use instructions like: `COPY target/*.jar /deployments` it using '/target' is a requirement.
            //For s2i kind of builds where jars are expected directly in the '/' we have to use null.
            String contextRoot = config.buildStrategy == BuildStrategy.DOCKER ? "target" : null;
            createContainerImage(
                    newClientBuilderWithoutHttp2(kubernetesClient.getConfiguration(),
                            kubernetesClientBuilder.getHttpClientFactory()),
                    openshiftYml.get(), config, contextRoot, out.getOutputDirectory(), nativeImage.getPath());
            artifactResultProducer.produce(new ArtifactResultBuildItem(null, "native-container", Collections.emptyMap()));
            containerImageBuilder.produce(new ContainerImageBuilderBuildItem(OPENSHIFT));
        }
    }

    public static void createContainerImage(KubernetesClientBuilder kubernetesClientBuilder,
            GeneratedFileSystemResourceBuildItem openshiftManifests,
            ContainerImageOpenshiftConfig openshiftConfig,
            String base,
            Path output,
            Path... additional) {

        File tar;
        try {
            File original = Packaging.packageFile(output, base, additional);
            //Let's rename the archive and give it a more descriptive name, as it may appear in the logs.
            tar = Files.createTempFile("quarkus-", "-openshift").toFile();
            Files.move(original.toPath(), tar.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new RuntimeException("Error creating the openshift binary build archive.", e);
        }

        try (KubernetesClient client = kubernetesClientBuilder.build()) {
            OpenShiftClient openShiftClient = toOpenshiftClient(client);
            KubernetesList kubernetesList = Serialization
                    .unmarshalAsList(new ByteArrayInputStream(openshiftManifests.getData()));

            List<HasMetadata> buildResources = kubernetesList.getItems().stream()
                    .filter(i -> i instanceof BuildConfig || i instanceof ImageStream || i instanceof Secret)
                    .collect(Collectors.toList());

            applyOpenshiftResources(openShiftClient, buildResources);
            openshiftBuild(buildResources, tar, openshiftConfig, kubernetesClientBuilder);
        } finally {
            try {
                tar.delete();
            } catch (Exception e) {
                LOG.warn("Unable to delete temporary file " + tar.toPath().toAbsolutePath(), e);
            }
        }
    }

    private static OpenShiftClient toOpenshiftClient(KubernetesClient client) {
        try {
            return client.adapt(OpenShiftClient.class);
        } catch (KubernetesClientException e) {
            KubernetesClientErrorHandler.handle(e);
            return null; // will never happen
        }
    }

    /**
     * Apply the openshift resources and wait until ImageStreamTags are created.
     *
     * @param client the client instance
     * @param buildResources resources to apply
     */
    private static void applyOpenshiftResources(OpenShiftClient client, List<HasMetadata> buildResources) {
        // Apply build resource requirements
        try {
            for (HasMetadata i : distinct(buildResources)) {
                deployResource(client, i);
                LOG.info("Applied: " + i.getKind() + " " + i.getMetadata().getName());
            }
            try {
                OpenshiftUtils.waitForImageStreamTags(client, buildResources, 2, TimeUnit.MINUTES);
            } catch (KubernetesClientException e) {
                //User may not have permission to get / list `ImageStreamTag` or this step may fail for any reason.
                //As this is not an integral part of the build we should catch and log.
                LOG.debug("Waiting for ImageStream tag failed. Ignoring.");
            }
        } catch (KubernetesClientException e) {
            KubernetesClientErrorHandler.handle(e);
        }
    }

    private static void openshiftBuild(List<HasMetadata> buildResources, File binaryFile,
            ContainerImageOpenshiftConfig openshiftConfig, KubernetesClientBuilder kubernetesClientBuilder) {
        distinct(buildResources).stream().filter(i -> i instanceof BuildConfig).map(i -> (BuildConfig) i)
                .forEach(bc -> {
                    Build build = startOpenshiftBuild(bc, binaryFile, openshiftConfig, kubernetesClientBuilder);
                    waitForOpenshiftBuild(build, openshiftConfig, kubernetesClientBuilder);
                });
    }

    /**
     * Performs the binary build of the specified {@link BuildConfig} with the given
     * binary input.
     *
     * @param buildConfig The build config
     * @param binaryFile The binary file
     * @param openshiftConfig The openshift configuration
     * @param kubernetesClientBuilder The kubernetes client builder
     */
    private static Build startOpenshiftBuild(BuildConfig buildConfig, File binaryFile,
            ContainerImageOpenshiftConfig openshiftConfig, KubernetesClientBuilder kubernetesClientBuilder) {
        try (KubernetesClient kubernetesClient = kubernetesClientBuilder.build()) {
            OpenShiftClient client = toOpenshiftClient(kubernetesClient);
            try {
                return client.buildConfigs().withName(buildConfig.getMetadata().getName())
                        .instantiateBinary()
                        .withTimeoutInMillis(openshiftConfig.buildTimeout.toMillis())
                        .fromFile(binaryFile);
            } catch (Exception e) {
                Optional<Build> running = buildsOf(client, buildConfig).stream().findFirst();
                if (running.isPresent()) {
                    LOG.warn("An exception: '" + e.getMessage()
                            + " ' occurred while instantiating the build, however the build has been started.");
                    return running.get();
                } else {
                    throw openshiftException(e);
                }
            }
        }
    }

    private static void waitForOpenshiftBuild(Build build, ContainerImageOpenshiftConfig openshiftConfig,
            KubernetesClientBuilder kubernetesClientBuilder) {

        while (isNew(build) || isPending(build) || isRunning(build)) {
            final String buildName = build.getMetadata().getName();
            try (KubernetesClient kubernetesClient = kubernetesClientBuilder.build()) {
                OpenShiftClient client = toOpenshiftClient(kubernetesClient);
                Build updated = client.builds().withName(buildName).get();
                if (updated == null) {
                    throw new IllegalStateException("Build:" + build.getMetadata().getName() + " is no longer present!");
                } else if (updated.getStatus() == null) {
                    throw new IllegalStateException("Build:" + build.getMetadata().getName() + " has no status!");
                } else if (isNew(updated) || isPending(updated) || isRunning(updated)) {
                    build = updated;
                    try (LogWatch w = client.builds().withName(buildName).withPrettyOutput().watchLog();
                            Reader reader = new InputStreamReader(w.getOutput())) {
                        display(reader, openshiftConfig.buildLogLevel);
                    } catch (IOException | KubernetesClientException ex) {
                        // This may happen if the LogWatch is closed while we are still reading.
                        // We shouldn't let the build fail, so let's log a warning and display last few lines of the log
                        LOG.warn("Log stream closed, redisplaying last " + LOG_TAIL_SIZE + " entries:");
                        try {
                            display(client.builds().withName(buildName).tailingLines(LOG_TAIL_SIZE).getLogReader(),
                                    Logger.Level.WARN);
                        } catch (IOException | KubernetesClientException ignored) {
                            // Let's ignore this.
                        }
                    }
                } else if (isComplete(updated)) {
                    return;
                } else if (isCancelled(updated)) {
                    throw new IllegalStateException("Build:" + buildName + " cancelled!");
                } else if (isFailed(updated)) {
                    throw new IllegalStateException(
                            "Build:" + buildName + " failed! " + updated.getStatus().getMessage());
                } else if (isError(updated)) {
                    throw new IllegalStateException(
                            "Build:" + buildName + " encountered error! " + updated.getStatus().getMessage());
                }
            }
        }
    }

    public static Predicate<HasMetadata> distinctByResourceKey() {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(t.getApiVersion() + "/" + t.getKind() + ":" + t.getMetadata().getName(),
                Boolean.TRUE) == null;
    }

    private static Collection<HasMetadata> distinct(Collection<HasMetadata> resources) {
        return resources.stream().filter(distinctByResourceKey()).collect(Collectors.toList());
    }

    private static List<Build> buildsOf(OpenShiftClient client, BuildConfig config) {
        return client.builds().withLabel(BUILD_CONFIG_NAME, config.getMetadata().getName()).list().getItems();
    }

    private static RuntimeException openshiftException(Throwable t) {
        if (t instanceof KubernetesClientException) {
            KubernetesClientErrorHandler.handle((KubernetesClientException) t);
        }
        return new RuntimeException("Execution of openshift build failed. See build output for more details", t);
    }

    private static void display(Reader logReader, Logger.Level level) throws IOException {
        BufferedReader reader = new BufferedReader(logReader);
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            LOG.log(level, line);
        }
    }

    private static KubernetesClientBuilder newClientBuilderWithoutHttp2(Config configuration,
            HttpClient.Factory httpClientFactory) {
        //Let's disable http2 as it causes issues with duplicate build triggers.
        configuration.setHttp2Disable(true);

        return new KubernetesClientBuilder().withConfig(configuration).withHttpClientFactory(httpClientFactory);
    }

    private static KubernetesClient buildClient(KubernetesClientBuildItem kubernetesClientBuilder) {
        getNamespace().ifPresent(kubernetesClientBuilder.getConfig()::setNamespace);
        return kubernetesClientBuilder.buildClient();
    }

    private static void deployResource(OpenShiftClient client, HasMetadata metadata) {
        DeployStrategy deployStrategy = getDeployStrategy();
        var r = client.resource(metadata);
        // Delete build config it already existed unless the deploy strategy is not create or update.
        if (deployStrategy != DeployStrategy.CreateOrUpdate && r instanceof BuildConfig) {
            deleteBuildConfig(client, metadata, r);
        }

        // If the image stream is already installed, we proceed with the next.
        if (r instanceof ImageStream) {
            ImageStream is = (ImageStream) r;
            ImageStream existing = client.imageStreams().withName(metadata.getMetadata().getName()).get();
            if (existing != null &&
                    existing.getSpec() != null &&
                    existing.getSpec().getDockerImageRepository() != null &&
                    existing.getSpec().getDockerImageRepository().equals(is.getSpec().getDockerImageRepository())) {
                LOG.info("Found: " + metadata.getKind() + " " + metadata.getMetadata().getName() + " repository: "
                        + existing.getSpec().getDockerImageRepository());
                return;
            }
        }

        // Deploy the current resource.
        switch (deployStrategy) {
            case Create:
                r.create();
                break;
            case Replace:
                r.replace();
                break;
            case ServerSideApply:
                r.patch(PatchContext.of(PatchType.SERVER_SIDE_APPLY));
                break;
            default:
                r.createOrReplace();
                break;
        }
    }

    private static void deleteBuildConfig(OpenShiftClient client, HasMetadata metadata, NamespaceableResource<HasMetadata> r) {
        r.cascading(true).delete();
        try {
            client.resource(metadata).waitUntilCondition(d -> d == null, 10, TimeUnit.SECONDS);
        } catch (IllegalArgumentException e) {
            // We should ignore that, as its expected to be thrown when item is actually
            // deleted.
        }
    }

    // visible for test
    static String concatUnixPaths(String... elements) {
        StringBuilder result = new StringBuilder();
        for (String element : elements) {
            if (element.endsWith("/")) {
                element = element.substring(0, element.length() - 1);
            }
            if (element.isEmpty()) {
                continue;
            }
            if (!element.startsWith("/") && result.length() > 0) {
                result.append('/');
            }
            result.append(element);
        }
        return result.toString();
    }

    static boolean isNew(Build build) {
        return build != null && build.getStatus() != null
                && BuildStatus.New.name().equalsIgnoreCase(build.getStatus().getPhase());
    }

    static boolean isPending(Build build) {
        return build != null && build.getStatus() != null
                && BuildStatus.Pending.name().equalsIgnoreCase(build.getStatus().getPhase());
    }

    static boolean isRunning(Build build) {
        return build != null && build.getStatus() != null
                && BuildStatus.Running.name().equalsIgnoreCase(build.getStatus().getPhase());
    }

    static boolean isComplete(Build build) {
        return build != null && build.getStatus() != null
                && BuildStatus.Complete.name().equalsIgnoreCase(build.getStatus().getPhase());
    }

    static boolean isFailed(Build build) {
        return build != null && build.getStatus() != null
                && BuildStatus.Failed.name().equalsIgnoreCase(build.getStatus().getPhase());
    }

    static boolean isError(Build build) {
        return build != null && build.getStatus() != null
                && BuildStatus.Error.name().equalsIgnoreCase(build.getStatus().getPhase());
    }

    static boolean isCancelled(Build build) {
        return build != null && build.getStatus() != null
                && BuildStatus.Cancelled.name().equalsIgnoreCase(build.getStatus().getPhase());
    }

}
