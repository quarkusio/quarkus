package io.quarkus.container.image.s2i.deployment;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
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
import io.quarkus.container.image.deployment.util.ImageUtil;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;
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
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.kubernetes.client.spi.KubernetesClientBuildItem;
import io.quarkus.kubernetes.spi.KubernetesEnvVarBuildItem;

public class S2iProcessor {

    private static final String JAR_ARTIFACT_FORMAT = "%s%s.jar";

    private static final Logger LOG = Logger.getLogger(S2iProcessor.class);

    @BuildStep(onlyIf = S2iBuild.class, onlyIfNot = NativeBuild.class)
    public void s2iRequirements(CurateOutcomeBuildItem curateOutcomeBuildItem,
            OutputTargetBuildItem out,
            PackageConfig packageConfig,
            BuildProducer<KubernetesEnvVarBuildItem> envProducer) {

        final List<AppDependency> appDeps = curateOutcomeBuildItem.getEffectiveModel().getUserDependencies();
        String classpath = appDeps.stream()
                .map(d -> "/deployments/lib/" + d.getArtifact().getGroupId() + "." + d.getArtifact().getPath().getFileName())
                .collect(Collectors.joining(File.pathSeparator));

        envProducer.produce(new KubernetesEnvVarBuildItem("JAVA_APP_JAR",
                "/deployments/" + String.format(JAR_ARTIFACT_FORMAT, out.getBaseName(), packageConfig.runnerSuffix)));
        envProducer.produce(new KubernetesEnvVarBuildItem("JAVA_CLASSPATH", classpath));
    }

    @BuildStep(onlyIf = { IsNormal.class, S2iBuild.class }, onlyIfNot = NativeBuild.class)
    public void s2iBuildFromJar(S2iConfig s2iConfig,
            KubernetesClientBuildItem kubernetesClient,
            ContainerImageInfoBuildItem containerImage,
            ArchiveRootBuildItem archiveRoot, OutputTargetBuildItem out, PackageConfig packageConfig,
            List<GeneratedFileSystemResourceBuildItem> generatedResources,
            BuildProducer<ArtifactResultBuildItem> artifactResultProducer,
            BuildProducer<ContainerImageResultBuildItem> containerImageResultProducer,
            // used to ensure that the jar has been built
            JarBuildItem jar) {

        LOG.info("Building s2i image for jar.");
        String image = containerImage.getImage();

        GeneratedFileSystemResourceBuildItem openshiftYml = generatedResources
                .stream()
                .filter(r -> r.getName().endsWith("kubernetes/openshift.yml"))
                .findFirst().orElseThrow(() -> new IllegalStateException("Could not find kubernetes/openshift.yml"));

        Path artifactPath = out.getOutputDirectory()
                .resolve(String.format(JAR_ARTIFACT_FORMAT, out.getBaseName(), packageConfig.runnerSuffix));

        LOG.info(
                "Creating tarball from:  " + out.getOutputDirectory() + " including " + artifactPath.toAbsolutePath().toString()
                        + " and " + out.getOutputDirectory().resolve("lib").toAbsolutePath().toString());

        createContainerImage(kubernetesClient, openshiftYml, out.getOutputDirectory(), artifactPath,
                out.getOutputDirectory().resolve("lib"));
        artifactResultProducer.produce(new ArtifactResultBuildItem(null, "jar-container", Collections.emptyMap()));
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
