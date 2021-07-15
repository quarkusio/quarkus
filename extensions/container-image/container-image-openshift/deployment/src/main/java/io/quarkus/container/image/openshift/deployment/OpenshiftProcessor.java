package io.quarkus.container.image.openshift.deployment;

import static io.quarkus.container.image.openshift.deployment.OpenshiftUtils.mergeConfig;
import static io.quarkus.container.util.PathsUtil.findMainSourcesRoot;
import static io.quarkus.deployment.pkg.steps.JarResultBuildStep.DEFAULT_FAST_JAR_DIRECTORY_NAME;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.logging.Logger;

import io.dekorate.utils.Clients;
import io.dekorate.utils.Packaging;
import io.dekorate.utils.Serialization;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import io.fabric8.openshift.api.model.Build;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.ImageStream;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.container.image.deployment.ContainerImageConfig;
import io.quarkus.container.image.deployment.util.ImageUtil;
import io.quarkus.container.spi.AvailableContainerImageExtensionBuildItem;
import io.quarkus.container.spi.BaseImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImageBuildRequestBuildItem;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImagePushRequestBuildItem;
import io.quarkus.deployment.IsNormalNotRemoteDev;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ArchiveRootBuildItem;
import io.quarkus.deployment.builditem.GeneratedFileSystemResourceBuildItem;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.builditem.JarBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.kubernetes.client.deployment.KubernetesClientErrorHandler;
import io.quarkus.kubernetes.client.spi.KubernetesClientBuildItem;
import io.quarkus.kubernetes.spi.DecoratorBuildItem;
import io.quarkus.kubernetes.spi.KubernetesCommandBuildItem;
import io.quarkus.kubernetes.spi.KubernetesEnvBuildItem;

public class OpenshiftProcessor {

    public static final String OPENSHIFT = "openshift";
    private static final String BUILD_CONFIG_NAME = "openshift.io/build-config.name";
    private static final String RUNNING = "Running";

    private static final Logger LOG = Logger.getLogger(OpenshiftProcessor.class);

    @BuildStep
    public AvailableContainerImageExtensionBuildItem availability() {
        return new AvailableContainerImageExtensionBuildItem(OPENSHIFT);
    }

    @BuildStep(onlyIf = { OpenshiftBuild.class }, onlyIfNot = NativeBuild.class)
    public void openshiftPrepareJvmDockerBuild(OpenshiftConfig openshiftConfig,
            S2iConfig s2iConfig,
            OutputTargetBuildItem out,
            BuildProducer<DecoratorBuildItem> decorator) {
        OpenshiftConfig config = mergeConfig(openshiftConfig, s2iConfig);
        if (config.buildStrategy == BuildStrategy.DOCKER) {
            decorator.produce(new DecoratorBuildItem(new ApplyDockerfileToBuildConfigDecorator(null,
                    findMainSourcesRoot(out.getOutputDirectory()).getValue().resolve(openshiftConfig.jvmDockerfile))));
            //When using the docker build strategy, we can't possibly know these values, so it's the image responsibility to work without them.
            decorator.produce(new DecoratorBuildItem(new RemoveEnvVarDecorator(null, "JAVA_APP_JAR")));
            decorator.produce(new DecoratorBuildItem(new RemoveEnvVarDecorator(null, "JAVA_APP_LIB")));
        }
    }

    @BuildStep(onlyIf = { OpenshiftBuild.class, NativeBuild.class })
    public void openshiftPrepareNativeDockerBuild(OpenshiftConfig openshiftConfig,
            S2iConfig s2iConfig,
            OutputTargetBuildItem out,
            BuildProducer<DecoratorBuildItem> decorator) {
        OpenshiftConfig config = mergeConfig(openshiftConfig, s2iConfig);
        if (config.buildStrategy == BuildStrategy.DOCKER) {
            decorator.produce(new DecoratorBuildItem(new ApplyDockerfileToBuildConfigDecorator(null,
                    findMainSourcesRoot(out.getOutputDirectory()).getValue().resolve(openshiftConfig.nativeDockerfile))));
        }
        //Let's remove this for all kinds of native build
        decorator.produce(new DecoratorBuildItem(new RemoveEnvVarDecorator(null, "JAVA_APP_JAR")));
        decorator.produce(new DecoratorBuildItem(new RemoveEnvVarDecorator(null, "JAVA_APP_LIB")));
    }

    @BuildStep(onlyIf = { IsNormalNotRemoteDev.class, OpenshiftBuild.class }, onlyIfNot = NativeBuild.class)
    public void openshiftRequirementsJvm(OpenshiftConfig openshiftConfig,
            S2iConfig s2iConfig,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            OutputTargetBuildItem out,
            PackageConfig packageConfig,
            JarBuildItem jarBuildItem,
            BuildProducer<DecoratorBuildItem> decorator,
            BuildProducer<KubernetesEnvBuildItem> envProducer,
            BuildProducer<BaseImageInfoBuildItem> builderImageProducer,
            BuildProducer<KubernetesCommandBuildItem> commandProducer) {

        OpenshiftConfig config = mergeConfig(openshiftConfig, s2iConfig);
        String outputJarFileName = jarBuildItem.getPath().getFileName().toString();
        String jarFileName = config.jarFileName.orElse(outputJarFileName);

        builderImageProducer.produce(new BaseImageInfoBuildItem(config.baseJvmImage));
        Optional<OpenshiftBaseJavaImage> baseImage = OpenshiftBaseJavaImage.findMatching(config.baseJvmImage);

        if (config.buildStrategy == BuildStrategy.BINARY) {
            // Jar directory priorities:
            // 1. explictly specified by the user.
            // 2. detected via OpenshiftBaseJavaImage
            // 3. fallback value
            String jarDirectory = config.jarDirectory
                    .orElse(baseImage.map(i -> i.getJarDirectory()).orElse(config.FALLBACK_JAR_DIRECTORY));
            String pathToJar = concatUnixPaths(jarDirectory, jarFileName);

            // If the image is known, we can define env vars for classpath, jar, lib etc.
            baseImage.ifPresent(b -> {
                envProducer.produce(KubernetesEnvBuildItem.createSimpleVar(b.getJarEnvVar(), pathToJar, null));
                envProducer.produce(KubernetesEnvBuildItem.createSimpleVar(b.getJvmOptionsEnvVar(),
                        String.join(" ", config.jvmArguments), null));
            });
            //In all other cases its the responsibility of the image to set those up correctly.
            if (!baseImage.isPresent()) {
                List<String> cmd = new ArrayList<>();
                cmd.add("java");
                cmd.addAll(config.jvmArguments);
                cmd.addAll(Arrays.asList("-jar", pathToJar));
                envProducer.produce(KubernetesEnvBuildItem.createSimpleVar("JAVA_APP_JAR", pathToJar, null));
                commandProducer.produce(KubernetesCommandBuildItem.command(cmd));
            }
        }
    }

    @BuildStep(onlyIf = { IsNormalNotRemoteDev.class, OpenshiftBuild.class, NativeBuild.class })
    public void openshiftRequirementsNative(OpenshiftConfig openshiftConfig,
            S2iConfig s2iConfig,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            OutputTargetBuildItem out,
            PackageConfig packageConfig,
            NativeImageBuildItem nativeImage,
            BuildProducer<KubernetesEnvBuildItem> envProducer,
            BuildProducer<BaseImageInfoBuildItem> builderImageProducer,
            BuildProducer<KubernetesCommandBuildItem> commandProducer) {

        OpenshiftConfig config = mergeConfig(openshiftConfig, s2iConfig);
        boolean usingDefaultBuilder = ImageUtil.getRepository(OpenshiftConfig.DEFAULT_BASE_NATIVE_IMAGE)
                .equals(ImageUtil.getRepository(config.baseNativeImage));
        String outputNativeBinaryFileName = nativeImage.getPath().getFileName().toString();

        String nativeBinaryFileName = null;

        //The default openshift builder for native builds, renames the native binary.
        //To make things easier for the user, we need to handle it.
        if (usingDefaultBuilder && !config.nativeBinaryFileName.isPresent()) {
            nativeBinaryFileName = OpenshiftConfig.DEFAULT_NATIVE_TARGET_FILENAME;
        } else {
            nativeBinaryFileName = config.nativeBinaryFileName.orElse(outputNativeBinaryFileName);
        }

        if (config.buildStrategy == BuildStrategy.BINARY) {
            builderImageProducer.produce(new BaseImageInfoBuildItem(config.baseNativeImage));
            Optional<OpenshiftBaseNativeImage> baseImage = OpenshiftBaseNativeImage.findMatching(config.baseNativeImage);
            // Native binary directory priorities:
            // 1. explictly specified by the user.
            // 2. detected via OpenshiftBaseNativeImage
            // 3. fallback value
            String nativeBinaryDirectory = config.nativeBinaryDirectory
                    .orElse(baseImage.map(i -> i.getNativeBinaryDirectory()).orElse(config.FALLBAC_NATIVE_BINARY_DIRECTORY));
            String pathToNativeBinary = concatUnixPaths(nativeBinaryDirectory, nativeBinaryFileName);

            baseImage.ifPresent(b -> {
                envProducer.produce(
                        KubernetesEnvBuildItem.createSimpleVar(b.getHomeDirEnvVar(), nativeBinaryDirectory, OPENSHIFT));
                envProducer.produce(KubernetesEnvBuildItem.createSimpleVar(b.getOptsEnvVar(),
                        String.join(" ", config.nativeArguments), OPENSHIFT));
            });

            if (!baseImage.isPresent()) {
                commandProducer.produce(KubernetesCommandBuildItem.commandWithArgs(pathToNativeBinary, config.nativeArguments));
            }
        }
    }

    @BuildStep(onlyIf = { IsNormalNotRemoteDev.class, OpenshiftBuild.class }, onlyIfNot = NativeBuild.class)
    public void openshiftBuildFromJar(OpenshiftConfig openshiftConfig,
            S2iConfig s2iConfig,
            ContainerImageConfig containerImageConfig,
            KubernetesClientBuildItem kubernetesClient,
            ContainerImageInfoBuildItem containerImage,
            ArchiveRootBuildItem archiveRoot, OutputTargetBuildItem out, PackageConfig packageConfig,
            List<GeneratedFileSystemResourceBuildItem> generatedResources,
            Optional<ContainerImageBuildRequestBuildItem> buildRequest,
            Optional<ContainerImagePushRequestBuildItem> pushRequest,
            BuildProducer<ArtifactResultBuildItem> artifactResultProducer,
            // used to ensure that the jar has been built
            JarBuildItem jar) {

        OpenshiftConfig config = mergeConfig(openshiftConfig, s2iConfig);
        if (!containerImageConfig.build && !containerImageConfig.push && !buildRequest.isPresent()
                && !pushRequest.isPresent()) {
            return;
        }

        Optional<GeneratedFileSystemResourceBuildItem> openshiftYml = generatedResources
                .stream()
                .filter(r -> r.getName().endsWith("kubernetes" + File.separator + "openshift.yml"))
                .findFirst();

        if (!openshiftYml.isPresent()) {
            LOG.warn(
                    "No Openshift manifests were generated (most likely due to the fact that the service is not an HTTP service) so no openshift process will be taking place");
            return;
        }

        String namespace = Optional.ofNullable(kubernetesClient.getClient().getNamespace()).orElse("default");
        LOG.info("Performing openshift binary build with jar on server: " + kubernetesClient.getClient().getMasterUrl()
                + " in namespace:" + namespace + ".");

        //The contextRoot is where inside the tarball we will add the jars. A null value means everything will be added under '/' while "target" means everything will be added under '/target'.
        //For docker kind of builds where we use instructions like: `COPY target/*.jar /deployments` it using '/target' is a requirement.
        //For s2i kind of builds where jars are expected directly in the '/' we have to use null.
        String outputDirName = out.getOutputDirectory().getFileName().toString();
        String contextRoot = getContextRoot(outputDirName, packageConfig.isFastJar(), config.buildStrategy);
        if (packageConfig.isFastJar()) {
            createContainerImage(kubernetesClient, openshiftYml.get(), config, contextRoot, jar.getPath().getParent(),
                    jar.getPath().getParent());
        } else if (jar.getLibraryDir() != null) { //When using uber-jar the libraryDir is going to be null, potentially causing NPE.
            createContainerImage(kubernetesClient, openshiftYml.get(), config, contextRoot, jar.getPath().getParent(),
                    jar.getPath(), jar.getLibraryDir());
        } else {
            createContainerImage(kubernetesClient, openshiftYml.get(), config, contextRoot, jar.getPath().getParent(),
                    jar.getPath());
        }
        artifactResultProducer.produce(new ArtifactResultBuildItem(null, "jar-container", Collections.emptyMap()));
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
    public void openshiftBuildFromNative(OpenshiftConfig openshiftConfig, S2iConfig s2iConfig,
            ContainerImageConfig containerImageConfig,
            KubernetesClientBuildItem kubernetesClient,
            ContainerImageInfoBuildItem containerImage,
            ArchiveRootBuildItem archiveRoot, OutputTargetBuildItem out, PackageConfig packageConfig,
            List<GeneratedFileSystemResourceBuildItem> generatedResources,
            Optional<ContainerImageBuildRequestBuildItem> buildRequest,
            Optional<ContainerImagePushRequestBuildItem> pushRequest,
            BuildProducer<ArtifactResultBuildItem> artifactResultProducer,
            NativeImageBuildItem nativeImage) {

        OpenshiftConfig config = mergeConfig(openshiftConfig, s2iConfig);
        if (!containerImageConfig.build && !containerImageConfig.push && !buildRequest.isPresent()
                && !pushRequest.isPresent()) {
            return;
        }

        String namespace = Optional.ofNullable(kubernetesClient.getClient().getNamespace()).orElse("default");
        LOG.info("Performing openshift binary build with native image on server: " + kubernetesClient.getClient().getMasterUrl()
                + " in namespace:" + namespace + ".");

        Optional<GeneratedFileSystemResourceBuildItem> openshiftYml = generatedResources
                .stream()
                .filter(r -> r.getName().endsWith("kubernetes" + File.separator + "openshift.yml"))
                .findFirst();

        if (!openshiftYml.isPresent()) {
            LOG.warn(
                    "No Openshift manifests were generated (most likely due to the fact that the service is not an HTTP service) so no openshift process will be taking place");
            return;
        }
        //The contextRoot is where inside the tarball we will add the jars. A null value means everything will be added under '/' while "target" means everything will be added under '/target'.
        //For docker kind of builds where we use instructions like: `COPY target/*.jar /deployments` it using '/target' is a requirement.
        //For s2i kind of builds where jars are expected directly in the '/' we have to use null.
        String contextRoot = config.buildStrategy == BuildStrategy.DOCKER ? "target" : null;
        createContainerImage(kubernetesClient, openshiftYml.get(), config, contextRoot, out.getOutputDirectory(),
                nativeImage.getPath());
        artifactResultProducer.produce(new ArtifactResultBuildItem(null, "native-container", Collections.emptyMap()));
    }

    public static void createContainerImage(KubernetesClientBuildItem kubernetesClient,
            GeneratedFileSystemResourceBuildItem openshiftManifests,
            OpenshiftConfig openshiftConfig,
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

        Config config = kubernetesClient.getClient().getConfiguration();
        //Let's disable http2 as it causes issues with duplicate build triggers.
        config.setHttp2Disable(true);
        try (KubernetesClient client = Clients.fromConfig(config)) {
            OpenShiftClient openShiftClient = toOpenshiftClient(client);
            KubernetesList kubernetesList = Serialization
                    .unmarshalAsList(new ByteArrayInputStream(openshiftManifests.getData()));

            List<HasMetadata> buildResources = kubernetesList.getItems().stream()
                    .filter(i -> i instanceof BuildConfig || i instanceof ImageStream || i instanceof Secret)
                    .collect(Collectors.toList());

            applyOpenshiftResources(openShiftClient, buildResources);
            openshiftBuild(openShiftClient, buildResources, tar, openshiftConfig);
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
                if (i instanceof BuildConfig) {
                    client.resource(i).cascading(true).delete();
                    try {
                        client.resource(i).waitUntilCondition(d -> d == null, 10, TimeUnit.SECONDS);
                    } catch (IllegalArgumentException e) {
                        // We should ignore that, as its expected to be thrown when item is actually
                        // deleted.
                    }
                } else if (i instanceof ImageStream) {
                    ImageStream is = (ImageStream) i;
                    ImageStream existing = client.imageStreams().withName(i.getMetadata().getName()).get();
                    if (existing != null &&
                            existing.getSpec() != null &&
                            existing.getSpec().getDockerImageRepository() != null &&
                            existing.getSpec().getDockerImageRepository().equals(is.getSpec().getDockerImageRepository())) {
                        LOG.info("Found: " + i.getKind() + " " + i.getMetadata().getName() + " repository: "
                                + existing.getSpec().getDockerImageRepository());
                        continue;
                    }
                }
                client.resource(i).createOrReplace();
                LOG.info("Applied: " + i.getKind() + " " + i.getMetadata().getName());
            }
            OpenshiftUtils.waitForImageStreamTags(client, buildResources, 2, TimeUnit.MINUTES);

        } catch (KubernetesClientException e) {
            KubernetesClientErrorHandler.handle(e);
        }
    }

    private static void openshiftBuild(OpenShiftClient client, List<HasMetadata> buildResources, File binaryFile,
            OpenshiftConfig openshiftConfig) {
        distinct(buildResources).stream().filter(i -> i instanceof BuildConfig).map(i -> (BuildConfig) i)
                .forEach(bc -> openshiftBuild(client, bc, binaryFile, openshiftConfig));
    }

    /**
     * Performs the binary build of the specified {@link BuildConfig} with the given
     * binary input.
     *
     * @param client The openshift client instance
     * @param buildConfig The build config
     * @param binaryFile The binary file
     * @param openshiftConfig The openshift configuration
     */
    private static void openshiftBuild(OpenShiftClient client, BuildConfig buildConfig, File binaryFile,
            OpenshiftConfig openshiftConfig) {
        Build build;
        try {
            build = client.buildConfigs().withName(buildConfig.getMetadata().getName())
                    .instantiateBinary()
                    .withTimeoutInMillis(openshiftConfig.buildTimeout.toMillis())
                    .fromFile(binaryFile);
        } catch (Exception e) {
            Optional<Build> running = runningBuildsOf(client, buildConfig).findFirst();
            if (running.isPresent()) {
                LOG.warn("An exception: '" + e.getMessage()
                        + " ' occurred while instantiating the build, however the build has been started.");
                build = running.get();
            } else {
                throw openshiftException(e);
            }
        }

        while (isNew(build) || isPending(build) || isRunning(build)) {
            Build updated = client.builds().withName(build.getMetadata().getName()).get();
            if (updated == null) {
                throw new IllegalStateException("Build:" + build.getMetadata().getName() + " is no longer present!");
            } else if (updated.getStatus() == null) {
                throw new IllegalStateException("Build:" + build.getMetadata().getName() + " has no status!");
            } else if (isNew(updated) || isPending(updated) || isRunning(updated)) {
                build = updated;
                final String buildName = build.getMetadata().getName();
                try (LogWatch w = client.builds().withName(build.getMetadata().getName()).withPrettyOutput().watchLog();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(w.getOutput()))) {
                    watchBuild(client, openshiftConfig, buildName, w);
                    for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                        LOG.info(line);
                    }
                } catch (IOException e) {
                    throw openshiftException(e);
                }
            } else if (isComplete(updated)) {
                return;
            } else if (isCancelled(updated)) {
                throw new IllegalStateException("Build:" + build.getMetadata().getName() + " cancelled!");
            } else if (isFailed(updated)) {
                throw new IllegalStateException(
                        "Build:" + build.getMetadata().getName() + " failed! " + updated.getStatus().getMessage());
            } else if (isError(updated)) {
                throw new IllegalStateException(
                        "Build:" + build.getMetadata().getName() + " encountered error! " + updated.getStatus().getMessage());
            }
        }
    }

    private static void watchBuild(OpenShiftClient client, OpenshiftConfig openshiftConfig, String buildName, Closeable watch) {
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                client.builds().withName(buildName).waitUntilCondition(b -> !RUNNING.equalsIgnoreCase(b.getStatus().getPhase()),
                        openshiftConfig.buildTimeout.toMillis(), TimeUnit.MILLISECONDS);
            } finally {
                try {
                    watch.close();
                } catch (IOException e) {
                    LOG.debug("Error closing log reader.");
                }
            }
        });
    }

    public static Predicate<HasMetadata> distictByResourceKey() {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(t.getApiVersion() + "/" + t.getKind() + ":" + t.getMetadata().getName(),
                Boolean.TRUE) == null;
    }

    private static Collection<HasMetadata> distinct(Collection<HasMetadata> resources) {
        return resources.stream().filter(distictByResourceKey()).collect(Collectors.toList());
    }

    private static List<Build> buildsOf(OpenShiftClient client, BuildConfig config) {
        return client.builds().withLabel(BUILD_CONFIG_NAME, config.getMetadata().getName()).list().getItems();
    }

    private static Stream<Build> runningBuildsOf(OpenShiftClient client, BuildConfig config) {
        return buildsOf(client, config).stream().filter(b -> RUNNING.equalsIgnoreCase(b.getStatus().getPhase()));
    }

    private static RuntimeException openshiftException(Throwable t) {
        if (t instanceof KubernetesClientException) {
            KubernetesClientErrorHandler.handle((KubernetesClientException) t);
        }
        return new RuntimeException("Execution of openshift build failed. See build output for more details", t);
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
