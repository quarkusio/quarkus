package io.quarkus.container.image.s2i.deployment;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.dekorate.deps.kubernetes.api.model.HasMetadata;
import io.dekorate.deps.kubernetes.api.model.KubernetesList;
import io.dekorate.deps.kubernetes.api.model.Secret;
import io.dekorate.deps.kubernetes.client.KubernetesClient;
import io.dekorate.deps.openshift.api.model.Build;
import io.dekorate.deps.openshift.api.model.BuildConfig;
import io.dekorate.deps.openshift.api.model.ImageStream;
import io.dekorate.deps.openshift.client.OpenShiftClient;
import io.dekorate.s2i.util.S2iUtils;
import io.dekorate.utils.Clients;
import io.dekorate.utils.Packaging;
import io.dekorate.utils.Serialization;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.container.image.deployment.ContainerImageConfig;
import io.quarkus.container.image.deployment.ContainerImageConfig.Execution;
import io.quarkus.container.image.deployment.util.ImageUtil;
import io.quarkus.container.spi.BaseImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImageBuildRequestBuildItem;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImagePushRequestBuildItem;
import io.quarkus.container.spi.ContainerImageResultBuildItem;
import io.quarkus.deployment.IsNormal;
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
import io.quarkus.kubernetes.client.spi.KubernetesClientBuildItem;
import io.quarkus.kubernetes.spi.KubernetesCommandBuildItem;

public class S2iProcessor {

    private static final String JAR_ARTIFACT_FORMAT = "%s%s.jar";
    private static final String NATIVE_ARTIFACT_FORMAT = "%s%s";

    private static final Logger LOG = Logger.getLogger(S2iProcessor.class);

    @BuildStep(onlyIf = S2iBuild.class, onlyIfNot = NativeBuild.class)
    public void s2iRequirementsJvm(S2iConfig s2iConfig,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            OutputTargetBuildItem out,
            PackageConfig packageConfig,
            BuildProducer<BaseImageInfoBuildItem> builderImageProducer,
            BuildProducer<KubernetesCommandBuildItem> commandProducer) {

        final List<AppDependency> appDeps = curateOutcomeBuildItem.getEffectiveModel().getUserDependencies();
        String classpath = appDeps.stream()
                .map(d -> Paths.get(s2iConfig.jarPath).getParent().resolve("lib")
                        .resolve(d.getArtifact().getGroupId() + "." + d.getArtifact().getPath().getFileName()).toAbsolutePath()
                        .toString())
                .collect(Collectors.joining(File.pathSeparator));

        List<String> args = new ArrayList<>();
        args.addAll(Arrays.asList("-jar", s2iConfig.jarPath, "-cp", classpath));
        args.addAll(s2iConfig.jvmArguments);

        if (!s2iConfig.hasDefaultBaseJvmImage()) {
            builderImageProducer.produce(new BaseImageInfoBuildItem(s2iConfig.baseJvmImage));
        }
        commandProducer.produce(new KubernetesCommandBuildItem("java", args.toArray(new String[args.size()])));
    }

    @BuildStep(onlyIf = { S2iBuild.class, NativeBuild.class })
    public void s2iRequirementsNative(S2iConfig s2iConfig,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            OutputTargetBuildItem out,
            PackageConfig packageConfig,
            BuildProducer<BaseImageInfoBuildItem> builderImageProducer,
            BuildProducer<KubernetesCommandBuildItem> commandProducer) {

        builderImageProducer.produce(new BaseImageInfoBuildItem(s2iConfig.baseNativeImage));
        commandProducer.produce(new KubernetesCommandBuildItem(s2iConfig.nativeBinaryPath,
                s2iConfig.nativeArguments.toArray(new String[s2iConfig.nativeArguments.size()])));
    }

    @BuildStep(onlyIf = { IsNormal.class, S2iBuild.class }, onlyIfNot = NativeBuild.class)
    public void s2iBuildFromJar(S2iConfig s2iConfig, ContainerImageConfig containerImageConfig,
            KubernetesClientBuildItem kubernetesClient,
            ContainerImageInfoBuildItem containerImage,
            ArchiveRootBuildItem archiveRoot, OutputTargetBuildItem out, PackageConfig packageConfig,
            List<GeneratedFileSystemResourceBuildItem> generatedResources,
            Optional<ContainerImageBuildRequestBuildItem> buildRequest,
            Optional<ContainerImagePushRequestBuildItem> pushRequest,
            BuildProducer<ArtifactResultBuildItem> artifactResultProducer,
            BuildProducer<ContainerImageResultBuildItem> containerImageResultProducer,
            // used to ensure that the jar has been built
            JarBuildItem jar) {

        if (containerImageConfig.execution == Execution.NONE && !buildRequest.isPresent() && !pushRequest.isPresent()) {
            return;
        }

        String namespace = Optional.ofNullable(kubernetesClient.getClient().getNamespace()).orElse("default");
        LOG.info("Performing s2i binary build with jar on server: " + kubernetesClient.getClient().getMasterUrl()
                + " in namespace:" + namespace + ".");
        String image = containerImage.getImage();

        GeneratedFileSystemResourceBuildItem openshiftYml = generatedResources
                .stream()
                .filter(r -> r.getName().endsWith("kubernetes/openshift.yml"))
                .findFirst().orElseThrow(() -> new IllegalStateException("Could not find kubernetes/openshift.yml"));

        Path artifactPath = out.getOutputDirectory()
                .resolve(String.format(JAR_ARTIFACT_FORMAT, out.getBaseName(), packageConfig.runnerSuffix));

        createContainerImage(kubernetesClient, openshiftYml, out.getOutputDirectory(), artifactPath,
                out.getOutputDirectory().resolve("lib"));
        artifactResultProducer.produce(new ArtifactResultBuildItem(null, "jar-container", Collections.emptyMap()));
        containerImageResultProducer.produce(
                new ContainerImageResultBuildItem(null, ImageUtil.getRepository(image), ImageUtil.getTag(image)));
    }

    @BuildStep(onlyIf = { IsNormal.class, S2iBuild.class, NativeBuild.class })
    public void s2iBuildFromNative(S2iConfig s2iConfig, ContainerImageConfig containerImageConfig,
            KubernetesClientBuildItem kubernetesClient,
            ContainerImageInfoBuildItem containerImage,
            ArchiveRootBuildItem archiveRoot, OutputTargetBuildItem out, PackageConfig packageConfig,
            List<GeneratedFileSystemResourceBuildItem> generatedResources,
            Optional<ContainerImageBuildRequestBuildItem> buildRequest,
            Optional<ContainerImagePushRequestBuildItem> pushRequest,
            BuildProducer<ArtifactResultBuildItem> artifactResultProducer,
            BuildProducer<ContainerImageResultBuildItem> containerImageResultProducer,
            NativeImageBuildItem nativeImageBuildItem) {

        if (containerImageConfig.execution == Execution.NONE && !buildRequest.isPresent() && !pushRequest.isPresent()) {
            return;
        }

        String namespace = Optional.ofNullable(kubernetesClient.getClient().getNamespace()).orElse("default");
        LOG.info("Performing s2i binary build with native image on server: " + kubernetesClient.getClient().getMasterUrl()
                + " in namespace:" + namespace + ".");

        String image = containerImage.getImage();

        GeneratedFileSystemResourceBuildItem openshiftYml = generatedResources
                .stream()
                .filter(r -> r.getName().endsWith("kubernetes/openshift.yml"))
                .findFirst().orElseThrow(() -> new IllegalStateException("Could not find kubernetes/openshift.yml"));

        Path artifactPath = out.getOutputDirectory()
                .resolve(String.format(NATIVE_ARTIFACT_FORMAT, out.getBaseName(), packageConfig.runnerSuffix));

        createContainerImage(kubernetesClient, openshiftYml, out.getOutputDirectory(), artifactPath);
        artifactResultProducer.produce(new ArtifactResultBuildItem(null, "native-container", Collections.emptyMap()));
        containerImageResultProducer.produce(
                new ContainerImageResultBuildItem(null, ImageUtil.getRepository(image), ImageUtil.getTag(image)));
    }

    public static void createContainerImage(KubernetesClientBuildItem kubernetesClient,
            GeneratedFileSystemResourceBuildItem openshiftManifests,
            Path output,
            Path... additional) {

        File tar = Packaging.packageFile(output, additional);
        KubernetesClient client = Clients.fromConfig(kubernetesClient.getClient().getConfiguration());
        KubernetesList kubernetesList = Serialization
                .unmarshalAsList(new ByteArrayInputStream(openshiftManifests.getData()));

        List<HasMetadata> buildResources = kubernetesList.getItems().stream()
                .filter(i -> i instanceof BuildConfig || i instanceof ImageStream || i instanceof Secret)
                .collect(Collectors.toList());

        applyS2iResources(client, buildResources);
        s2iBuild(client, buildResources, tar);
    }

    /**
     * Apply the s2i resources and wait until ImageStreamTags are created.
     *
     * @param client the client instance
     * @param the resources to apply
     */
    private static void applyS2iResources(KubernetesClient client, List<HasMetadata> buildResources) {
        // Apply build resource requirements
        buildResources.forEach(i -> {
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
            }
            client.resource(i).createOrReplace();
            LOG.info("Applied: " + i.getKind() + " " + i.getMetadata().getName());
        });
        S2iUtils.waitForImageStreamTags(buildResources, 2, TimeUnit.MINUTES);
    }

    private static void s2iBuild(KubernetesClient client, List<HasMetadata> buildResources, File binaryFile) {
        buildResources.stream().filter(i -> i instanceof BuildConfig).map(i -> (BuildConfig) i)
                .forEach(bc -> s2iBuild(client.adapt(OpenShiftClient.class), bc, binaryFile));
    }

    /**
     * Performs the binary build of the specified {@link BuildConfig} with the given
     * binary input.
     *
     * @param buildConfig The build config.
     * @param binaryFile The binary file.
     */
    private static void s2iBuild(OpenShiftClient client, BuildConfig buildConfig, File binaryFile) {
        Build build = client.buildConfigs().withName(buildConfig.getMetadata().getName()).instantiateBinary()
                .fromFile(binaryFile);
        try (BufferedReader reader = new BufferedReader(
                client.builds().withName(build.getMetadata().getName()).getLogReader())) {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                System.out.println(line);
            }
        } catch (IOException e) {
            s2iException(e);
        }
    }

    private static RuntimeException s2iException(Throwable t) {
        return new RuntimeException("Execution of s2i build failed. See s2i output for more details", t);
    }
}
