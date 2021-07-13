package io.quarkus.container.image.s2i.deployment;

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
import io.quarkus.bootstrap.model.AppDependency;
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
import io.quarkus.kubernetes.spi.KubernetesCommandBuildItem;
import io.quarkus.kubernetes.spi.KubernetesEnvBuildItem;

public class S2iProcessor {

    public static final String S2I = "s2i";
    private static final String OPENSHIFT = "openshift";
    private static final String BUILD_CONFIG_NAME = "openshift.io/build-config.name";
    private static final String RUNNING = "Running";

    private static final Logger LOG = Logger.getLogger(S2iProcessor.class);

    @BuildStep
    public AvailableContainerImageExtensionBuildItem availability() {
        return new AvailableContainerImageExtensionBuildItem(S2I);
    }

    @BuildStep(onlyIf = { IsNormalNotRemoteDev.class, S2iBuild.class }, onlyIfNot = NativeBuild.class)
    public void s2iRequirementsJvm(S2iConfig s2iConfig,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            OutputTargetBuildItem out,
            PackageConfig packageConfig,
            JarBuildItem jarBuildItem,
            BuildProducer<KubernetesEnvBuildItem> envProducer,
            BuildProducer<BaseImageInfoBuildItem> builderImageProducer,
            BuildProducer<KubernetesCommandBuildItem> commandProducer) {

        final List<AppDependency> appDeps = curateOutcomeBuildItem.getEffectiveModel().getUserDependencies();
        String outputJarFileName = jarBuildItem.getPath().getFileName().toString();
        String classpath = appDeps.stream()
                .map(d -> d.getArtifact().getGroupId() + "." + d.getArtifact().getPath().getFileName())
                .map(s -> concatUnixPaths(s2iConfig.jarDirectory, "lib", s))
                .collect(Collectors.joining(":"));

        String jarFileName = s2iConfig.jarFileName.orElse(outputJarFileName);
        String jarDirectory = s2iConfig.jarDirectory;
        String pathToJar = concatUnixPaths(jarDirectory, jarFileName);

        builderImageProducer.produce(new BaseImageInfoBuildItem(s2iConfig.baseJvmImage));
        Optional<S2iBaseJavaImage> baseImage = S2iBaseJavaImage.findMatching(s2iConfig.baseJvmImage);

        baseImage.ifPresent(b -> {
            envProducer.produce(KubernetesEnvBuildItem.createSimpleVar(b.getJarEnvVar(), pathToJar, OPENSHIFT));
            envProducer.produce(KubernetesEnvBuildItem.createSimpleVar(b.getJarLibEnvVar(),
                    concatUnixPaths(jarDirectory, "lib"), OPENSHIFT));
            envProducer.produce(KubernetesEnvBuildItem.createSimpleVar(b.getClasspathEnvVar(), classpath, OPENSHIFT));
            envProducer.produce(KubernetesEnvBuildItem.createSimpleVar(b.getJvmOptionsEnvVar(),
                    String.join(" ", s2iConfig.jvmArguments), OPENSHIFT));
        });

        if (!baseImage.isPresent()) {
            List<String> cmd = new ArrayList<>();
            cmd.add("java");
            cmd.addAll(s2iConfig.jvmArguments);
            cmd.addAll(Arrays.asList("-jar", pathToJar, "-cp", classpath));
            commandProducer.produce(KubernetesCommandBuildItem.command(cmd));
        }
    }

    @BuildStep(onlyIf = { IsNormalNotRemoteDev.class, S2iBuild.class, NativeBuild.class })
    public void s2iRequirementsNative(S2iConfig s2iConfig,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            OutputTargetBuildItem out,
            PackageConfig packageConfig,
            NativeImageBuildItem nativeImage,
            BuildProducer<KubernetesEnvBuildItem> envProducer,
            BuildProducer<BaseImageInfoBuildItem> builderImageProducer,
            BuildProducer<KubernetesCommandBuildItem> commandProducer) {

        boolean usingDefaultBuilder = ImageUtil.getRepository(S2iConfig.DEFAULT_BASE_NATIVE_IMAGE)
                .equals(ImageUtil.getRepository(s2iConfig.baseNativeImage));
        String outputNativeBinaryFileName = nativeImage.getPath().getFileName().toString();

        String nativeBinaryFileName = null;

        //The default s2i builder for native builds, renames the native binary.
        //To make things easier for the user, we need to handle it.
        if (usingDefaultBuilder && !s2iConfig.nativeBinaryFileName.isPresent()) {
            nativeBinaryFileName = S2iConfig.DEFAULT_NATIVE_TARGET_FILENAME;
        } else {
            nativeBinaryFileName = s2iConfig.nativeBinaryFileName.orElse(outputNativeBinaryFileName);
        }

        String pathToNativeBinary = concatUnixPaths(s2iConfig.nativeBinaryDirectory, nativeBinaryFileName);

        builderImageProducer.produce(new BaseImageInfoBuildItem(s2iConfig.baseNativeImage));
        Optional<S2iBaseNativeImage> baseImage = S2iBaseNativeImage.findMatching(s2iConfig.baseNativeImage);
        List<String> nativeArguments = s2iConfig.nativeArguments.orElse(Collections.emptyList());
        baseImage.ifPresent(b -> {
            envProducer.produce(
                    KubernetesEnvBuildItem.createSimpleVar(b.getHomeDirEnvVar(), s2iConfig.nativeBinaryDirectory, OPENSHIFT));
            envProducer.produce(
                    KubernetesEnvBuildItem.createSimpleVar(b.getOptsEnvVar(), String.join(" ", nativeArguments),
                            OPENSHIFT));
        });

        if (!baseImage.isPresent()) {
            commandProducer.produce(KubernetesCommandBuildItem.commandWithArgs(pathToNativeBinary, nativeArguments));
        }
    }

    @BuildStep(onlyIf = { IsNormalNotRemoteDev.class, S2iBuild.class }, onlyIfNot = NativeBuild.class)
    public void s2iBuildFromJar(S2iConfig s2iConfig, ContainerImageConfig containerImageConfig,
            KubernetesClientBuildItem kubernetesClient,
            ContainerImageInfoBuildItem containerImage,
            ArchiveRootBuildItem archiveRoot, OutputTargetBuildItem out, PackageConfig packageConfig,
            List<GeneratedFileSystemResourceBuildItem> generatedResources,
            Optional<ContainerImageBuildRequestBuildItem> buildRequest,
            Optional<ContainerImagePushRequestBuildItem> pushRequest,
            BuildProducer<ArtifactResultBuildItem> artifactResultProducer,
            // used to ensure that the jar has been built
            JarBuildItem jar) {

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
                    "No Openshift manifests were generated (most likely due to the fact that the service is not an HTTP service) so no s2i process will be taking place");
            return;
        }

        String namespace = Optional.ofNullable(kubernetesClient.getClient().getNamespace()).orElse("default");
        LOG.info("Performing s2i binary build with jar on server: " + kubernetesClient.getClient().getMasterUrl()
                + " in namespace:" + namespace + ".");

        createContainerImage(kubernetesClient, openshiftYml.get(), s2iConfig, out.getOutputDirectory(), jar.getPath(),
                out.getOutputDirectory().resolve("lib"));
        artifactResultProducer.produce(new ArtifactResultBuildItem(null, "jar-container", Collections.emptyMap()));
    }

    @BuildStep(onlyIf = { IsNormalNotRemoteDev.class, S2iBuild.class, NativeBuild.class })
    public void s2iBuildFromNative(S2iConfig s2iConfig, ContainerImageConfig containerImageConfig,
            KubernetesClientBuildItem kubernetesClient,
            ContainerImageInfoBuildItem containerImage,
            ArchiveRootBuildItem archiveRoot, OutputTargetBuildItem out, PackageConfig packageConfig,
            List<GeneratedFileSystemResourceBuildItem> generatedResources,
            Optional<ContainerImageBuildRequestBuildItem> buildRequest,
            Optional<ContainerImagePushRequestBuildItem> pushRequest,
            BuildProducer<ArtifactResultBuildItem> artifactResultProducer,
            NativeImageBuildItem nativeImage) {

        if (!containerImageConfig.build && !containerImageConfig.push && !buildRequest.isPresent()
                && !pushRequest.isPresent()) {
            return;
        }

        String namespace = Optional.ofNullable(kubernetesClient.getClient().getNamespace()).orElse("default");
        LOG.info("Performing s2i binary build with native image on server: " + kubernetesClient.getClient().getMasterUrl()
                + " in namespace:" + namespace + ".");

        Optional<GeneratedFileSystemResourceBuildItem> openshiftYml = generatedResources
                .stream()
                .filter(r -> r.getName().endsWith("kubernetes" + File.separator + "openshift.yml"))
                .findFirst();

        if (!openshiftYml.isPresent()) {
            LOG.warn(
                    "No Openshift manifests were generated (most likely due to the fact that the service is not an HTTP service) so no s2i process will be taking place");
            return;
        }

        createContainerImage(kubernetesClient, openshiftYml.get(), s2iConfig, out.getOutputDirectory(),
                nativeImage.getPath());
        artifactResultProducer.produce(new ArtifactResultBuildItem(null, "native-container", Collections.emptyMap()));
    }

    public static void createContainerImage(KubernetesClientBuildItem kubernetesClient,
            GeneratedFileSystemResourceBuildItem openshiftManifests,
            S2iConfig s2iConfig,
            Path output,
            Path... additional) {

        File tar;
        try {
            File original = PackageUtil.packageFile(output, additional);
            //Let's rename the archive and give it a more descriptive name, as it may appear in the logs.
            tar = Files.createTempFile("quarkus-", "-s2i").toFile();
            Files.move(original.toPath(), tar.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new RuntimeException("Error creating the s2i binary build archive.", e);
        }

        Config config = kubernetesClient.getClient().getConfiguration();
        //Let's disable http2 as it causes issues with duplicate build triggers.
        config.setHttp2Disable(true);
        try (KubernetesClient client = Clients.fromConfig(config)) {
            OpenShiftClient openShiftClient = client.adapt(OpenShiftClient.class);
            KubernetesList kubernetesList = Serialization
                    .unmarshalAsList(new ByteArrayInputStream(openshiftManifests.getData()));

            List<HasMetadata> buildResources = kubernetesList.getItems().stream()
                    .filter(i -> i instanceof BuildConfig || i instanceof ImageStream || i instanceof Secret)
                    .collect(Collectors.toList());

            applyS2iResources(openShiftClient, buildResources);
            s2iBuild(openShiftClient, buildResources, tar, s2iConfig);
        }
    }

    /**
     * Apply the s2i resources and wait until ImageStreamTags are created.
     *
     * @param client the client instance
     * @param buildResources resources to apply
     */
    private static void applyS2iResources(OpenShiftClient client, List<HasMetadata> buildResources) {
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
                    } catch (InterruptedException e) {
                        s2iException(e);
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
            S2iUtils.waitForImageStreamTags(client, buildResources, 2, TimeUnit.MINUTES);

        } catch (KubernetesClientException e) {
            KubernetesClientErrorHandler.handle(e);
        }
    }

    private static void s2iBuild(OpenShiftClient client, List<HasMetadata> buildResources, File binaryFile,
            S2iConfig s2iConfig) {
        distinct(buildResources).stream().filter(i -> i instanceof BuildConfig).map(i -> (BuildConfig) i)
                .forEach(bc -> s2iBuild(client, bc, binaryFile, s2iConfig));
    }

    /**
     * Performs the binary build of the specified {@link BuildConfig} with the given
     * binary input.
     *
     * @param client The openshift client instance
     * @param buildConfig The build config
     * @param binaryFile The binary file
     * @param s2iConfig The s2i configuration
     */
    private static void s2iBuild(OpenShiftClient client, BuildConfig buildConfig, File binaryFile, S2iConfig s2iConfig) {
        Build build;
        try {
            build = client.buildConfigs().withName(buildConfig.getMetadata().getName())
                    .instantiateBinary()
                    .withTimeoutInMillis(s2iConfig.buildTimeout.toMillis())
                    .fromFile(binaryFile);
        } catch (Exception e) {
            Optional<Build> running = runningBuildsOf(client, buildConfig).findFirst();
            if (running.isPresent()) {
                LOG.warn("An exception: '" + e.getMessage()
                        + " ' occurred while instantiating the build, however the build has been started.");
                build = running.get();
            } else {
                throw s2iException(e);
            }
        }

        final String buildName = build.getMetadata().getName();
        try (LogWatch w = client.builds().withName(build.getMetadata().getName()).withPrettyOutput().watchLog();
                BufferedReader reader = new BufferedReader(new InputStreamReader(w.getOutput()))) {
            waitForBuildComplete(client, s2iConfig, buildName, w);
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                LOG.info(line);
            }
        } catch (IOException e) {
            throw s2iException(e);
        }
    }

    private static void waitForBuildComplete(OpenShiftClient client, S2iConfig s2iConfig, String buildName, Closeable watch) {
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                client.builds().withName(buildName).waitUntilCondition(b -> !RUNNING.equalsIgnoreCase(b.getStatus().getPhase()),
                        s2iConfig.buildTimeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                s2iException(e);
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

    private static RuntimeException s2iException(Throwable t) {
        if (t instanceof KubernetesClientException) {
            KubernetesClientErrorHandler.handle((KubernetesClientException) t);
        }
        return new RuntimeException("Execution of s2i build failed. See s2i output for more details", t);
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
}
