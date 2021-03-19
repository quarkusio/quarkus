package io.quarkus.container.image.buildpack.deployment;

import static io.quarkus.container.util.PathsUtil.findMainSourcesRoot;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.hc.core5.http.nio.support.BasicPushProducer;
import org.jboss.logging.Logger;

import dev.snowdrop.buildpack.BuildPackBuilder;
import io.quarkus.container.image.deployment.ContainerImageConfig;
import io.quarkus.container.image.deployment.util.NativeBinaryUtil;
import io.quarkus.container.spi.AvailableContainerImageExtensionBuildItem;
import io.quarkus.container.spi.ContainerImageBuildRequestBuildItem;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImageLabelBuildItem;
import io.quarkus.container.spi.ContainerImagePushRequestBuildItem;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.IsNormalNotRemoteDev;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.MainClassBuildItem;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.AppCDSResultBuildItem;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.builditem.JarBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;

public class BuildpackProcessor {

    private static final Logger log = Logger.getLogger(BuildpackProcessor.class);

    public static final String BUILDPACK = "buildpack";

    private enum ProjectDirs {
        TARGET,
        SOURCE,
        ROOT
    };

    @BuildStep
    public AvailableContainerImageExtensionBuildItem availability() {
        return new AvailableContainerImageExtensionBuildItem(BUILDPACK);
    }

    @BuildStep(onlyIf = BuildpackBuild.class)
    public CapabilityBuildItem capability() {
        return new CapabilityBuildItem(Capability.CONTAINER_IMAGE_BUILDPACK);
    }

    @BuildStep(onlyIf = { IsNormalNotRemoteDev.class, BuildpackBuild.class }, onlyIfNot = NativeBuild.class)
    public void buildFromJar(ContainerImageConfig containerImageConfig, BuildpackConfig buildpackConfig,
            PackageConfig packageConfig,
            ContainerImageInfoBuildItem containerImage,
            JarBuildItem sourceJar,
            MainClassBuildItem mainClass,
            OutputTargetBuildItem outputTarget,
            CurateOutcomeBuildItem curateOutcome,
            Optional<ContainerImageBuildRequestBuildItem> buildRequest,
            Optional<ContainerImagePushRequestBuildItem> pushRequest,
            List<ContainerImageLabelBuildItem> containerImageLabels,
            Optional<AppCDSResultBuildItem> appCDSResult,
            BuildProducer<ArtifactResultBuildItem> artifactResultProducer) {

        if (!containerImageConfig.build && !containerImageConfig.push && !buildRequest.isPresent()
                && !pushRequest.isPresent()) {
            return;
        }

        Map<ProjectDirs, Path> dirs = getPaths(outputTarget);

        log.info("Using target dir of " + dirs.get(ProjectDirs.TARGET));
        log.info("Using source root of " + dirs.get(ProjectDirs.SOURCE));
        log.info("Using project root of " + dirs.get(ProjectDirs.ROOT));

        String targetImageName = containerImage.getImage().toString();
        log.info("Using Destination image of " + targetImageName);
        
        log.info("Using Builder image of " + buildpackConfig.builderImage);

        try {
            log.info("Configuring Buildpack build");
            
            BuildPackBuilder bpb = BuildPackBuilder.get()
                    .withContent(dirs.get(ProjectDirs.ROOT).toFile())
                    .withFinalImage(targetImageName)
                    .withBuildImage(buildpackConfig.builderImage);
            
            if(buildpackConfig.runImage.isPresent()) {
                log.info("Using Run image of " + buildpackConfig.runImage.get());
                bpb = bpb.withRunImage(buildpackConfig.runImage.get());
            }
            if(buildpackConfig.logLevel.isPresent()) {
                log.info("Using Buildpack loglevel of " + buildpackConfig.logLevel.get());
                bpb = bpb.withLogLevel(buildpackConfig.logLevel.get());
            }
            if(buildpackConfig.dockerHost.isPresent()) {
                log.info("Using DockerHost of " + buildpackConfig.dockerHost.get());
                bpb = bpb.withDockerHost(buildpackConfig.dockerHost.get());
            }
            if(buildpackConfig.pullTimeoutSeconds.isPresent()) {
                log.info("Using Docker pull timeout of " + buildpackConfig.pullTimeoutSeconds.get());
                bpb = bpb.withPullTimeout(buildpackConfig.pullTimeoutSeconds.get());
            }
            
            log.info("Initiating Buildpack build");
            int rc = bpb.build(new BuildpackLogReader());
            if (rc != 0) {
                throw new RuntimeException("Buildpack build failed with rc=" + rc);
            }
            
            log.info("Buildpack build complete");
            
        } catch (Exception e) {
            throw new RuntimeException("Buildpack build failed : ", e);
        }

        artifactResultProducer.produce(new ArtifactResultBuildItem(null, "jar-container",
                Collections.singletonMap("container-image", targetImageName)));
    }

    @BuildStep(onlyIf = { IsNormalNotRemoteDev.class, BuildpackBuild.class, NativeBuild.class })
    public void buildFromNative(ContainerImageConfig containerImageConfig, BuildpackConfig buildpackConfig,
            ContainerImageInfoBuildItem containerImage,
            NativeImageBuildItem nativeImage,
            OutputTargetBuildItem outputTarget,
            Optional<ContainerImageBuildRequestBuildItem> buildRequest,
            Optional<ContainerImagePushRequestBuildItem> pushRequest,
            List<ContainerImageLabelBuildItem> containerImageLabels,
            BuildProducer<ArtifactResultBuildItem> artifactResultProducer) {

        if (!containerImageConfig.build && !containerImageConfig.push && !buildRequest.isPresent()
                && !pushRequest.isPresent()) {
            return;
        }

        if (!NativeBinaryUtil.nativeIsLinuxBinary(nativeImage)) {
            throw new RuntimeException(
                    "The native binary produced by the build is not a Linux binary and therefore cannot be used in a Linux container image. Consider adding \"quarkus.native.container-build=true\" to your configuration");
        }

        //TODO: not implemented yet =)
        //      will most likely be a clone of the non-native, but with different builder image (for now). 

        String builtImageName = "fish";
        artifactResultProducer.produce(new ArtifactResultBuildItem(null, "native-container",
                Collections.singletonMap("container-image", builtImageName)));
    }

    private Map<ProjectDirs, Path> getPaths(OutputTargetBuildItem outputTarget) {

        Path targetDirectory = outputTarget.getOutputDirectory();

        Map.Entry<Path, Path> mainSourcesRoot = findMainSourcesRoot(targetDirectory);
        if (mainSourcesRoot == null) {
            throw new RuntimeException("Buildpack build unable to determine project dir");
        }
        Path sourceRoot = mainSourcesRoot.getKey();
        Path projectRoot = mainSourcesRoot.getValue();
        if (!projectRoot.toFile().exists() || !sourceRoot.toFile().exists()) {
            throw new RuntimeException("Buildpack build unable to verify project dir");
        }

        Map<ProjectDirs, Path> result = new HashMap<>();
        result.put(ProjectDirs.ROOT, projectRoot);
        result.put(ProjectDirs.SOURCE, sourceRoot);
        result.put(ProjectDirs.TARGET, targetDirectory);
        return result;
    }

}
