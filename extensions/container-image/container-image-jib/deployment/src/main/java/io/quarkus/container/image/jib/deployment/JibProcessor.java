package io.quarkus.container.image.jib.deployment;

import static io.quarkus.container.util.PathsUtil.findMainSourcesRoot;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import com.google.cloud.tools.jib.api.Containerizer;
import com.google.cloud.tools.jib.api.DockerDaemonImage;
import com.google.cloud.tools.jib.api.ImageReference;
import com.google.cloud.tools.jib.api.InvalidImageReferenceException;
import com.google.cloud.tools.jib.api.JavaContainerBuilder;
import com.google.cloud.tools.jib.api.Jib;
import com.google.cloud.tools.jib.api.JibContainer;
import com.google.cloud.tools.jib.api.JibContainerBuilder;
import com.google.cloud.tools.jib.api.LogEvent;
import com.google.cloud.tools.jib.api.RegistryImage;
import com.google.cloud.tools.jib.api.buildplan.AbsoluteUnixPath;
import com.google.cloud.tools.jib.api.buildplan.FileEntriesLayer;
import com.google.cloud.tools.jib.api.buildplan.FilePermissions;
import com.google.cloud.tools.jib.api.buildplan.Port;
import com.google.cloud.tools.jib.frontend.CredentialRetrieverFactory;

import io.quarkus.bootstrap.util.ZipUtils;
import io.quarkus.builder.Version;
import io.quarkus.container.image.deployment.ContainerImageConfig;
import io.quarkus.container.image.deployment.util.NativeBinaryUtil;
import io.quarkus.container.spi.ContainerImageBuildRequestBuildItem;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImageLabelBuildItem;
import io.quarkus.container.spi.ContainerImagePushRequestBuildItem;
import io.quarkus.container.util.PathsUtil;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.MainClassBuildItem;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.JarBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.pkg.steps.JarResultBuildStep;
import io.quarkus.deployment.pkg.steps.NativeBuild;

public class JibProcessor {

    private static final Logger log = Logger.getLogger(JibProcessor.class);

    public static final String JIB = "jib";
    private static final IsClassPredicate IS_CLASS_PREDICATE = new IsClassPredicate();
    private static final String BINARY_NAME_IN_CONTAINER = "application";

    @BuildStep(onlyIf = JibBuild.class)
    public CapabilityBuildItem capability() {
        return new CapabilityBuildItem(Capability.CONTAINER_IMAGE_JIB);
    }

    @BuildStep(onlyIf = { IsNormal.class, JibBuild.class }, onlyIfNot = NativeBuild.class)
    public void buildFromJar(ContainerImageConfig containerImageConfig, JibConfig jibConfig,
            PackageConfig packageConfig,
            ContainerImageInfoBuildItem containerImage,
            JarBuildItem sourceJar,
            MainClassBuildItem mainClass,
            OutputTargetBuildItem outputTarget,
            Optional<ContainerImageBuildRequestBuildItem> buildRequest,
            Optional<ContainerImagePushRequestBuildItem> pushRequest,
            List<ContainerImageLabelBuildItem> containerImageLabels,
            BuildProducer<ArtifactResultBuildItem> artifactResultProducer) {

        if (!containerImageConfig.build && !containerImageConfig.push && !buildRequest.isPresent()
                && !pushRequest.isPresent()) {
            return;
        }

        JibContainerBuilder jibContainerBuilder;
        String packageType = packageConfig.type;
        if (packageConfig.isLegacyJar() || packageType.equalsIgnoreCase(PackageConfig.UBER_JAR)) {
            jibContainerBuilder = createContainerBuilderFromLegacyJar(jibConfig,
                    sourceJar, outputTarget, mainClass, containerImageLabels);
        } else if (packageConfig.isFastJar()) {
            jibContainerBuilder = createContainerBuilderFromFastJar(jibConfig, sourceJar, containerImageLabels);
        } else {
            throw new IllegalArgumentException(
                    "Package type '" + packageType + "' is not supported by the container-image-jib extension");
        }
        setUser(jibConfig, jibContainerBuilder);
        handleExtraFiles(outputTarget, jibContainerBuilder);
        containerize(containerImageConfig, containerImage, jibContainerBuilder,
                pushRequest.isPresent());

        artifactResultProducer.produce(new ArtifactResultBuildItem(null, "jar-container", Collections.emptyMap()));
    }

    @BuildStep(onlyIf = { IsNormal.class, JibBuild.class, NativeBuild.class })
    public void buildFromNative(ContainerImageConfig containerImageConfig, JibConfig jibConfig,
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

        JibContainerBuilder jibContainerBuilder = createContainerBuilderFromNative(containerImageConfig, jibConfig,
                nativeImage, containerImageLabels);
        setUser(jibConfig, jibContainerBuilder);
        handleExtraFiles(outputTarget, jibContainerBuilder);
        containerize(containerImageConfig, containerImage, jibContainerBuilder,
                pushRequest.isPresent());

        artifactResultProducer.produce(new ArtifactResultBuildItem(null, "native-container", Collections.emptyMap()));
    }

    private JibContainer containerize(ContainerImageConfig containerImageConfig,
            ContainerImageInfoBuildItem containerImage, JibContainerBuilder jibContainerBuilder, boolean pushRequested) {
        Containerizer containerizer = createContainerizer(containerImageConfig, containerImage, pushRequested);
        for (String additionalTag : containerImage.getAdditionalTags()) {
            containerizer.withAdditionalTag(additionalTag);
        }
        try {
            log.info("Starting container image build");
            JibContainer container = jibContainerBuilder.containerize(containerizer);
            log.infof("%s container image %s (%s)\n",
                    containerImageConfig.push ? "Pushed" : "Created",
                    container.getTargetImage(),
                    container.getDigest());
            return container;
        } catch (Exception e) {
            throw new RuntimeException("Unable to create container image", e);
        }
    }

    private Containerizer createContainerizer(ContainerImageConfig containerImageConfig,
            ContainerImageInfoBuildItem containerImage,
            boolean pushRequested) {
        Containerizer containerizer;
        ImageReference imageReference = ImageReference.of(containerImage.getRegistry().orElse(null),
                containerImage.getRepository(), containerImage.getTag());

        if (pushRequested || containerImageConfig.push) {
            if (!containerImageConfig.registry.isPresent()) {
                log.info("No container image registry was set, so 'docker.io' will be used");
            }
            RegistryImage registryImage = toRegistryImage(imageReference, containerImageConfig.username,
                    containerImageConfig.password);
            containerizer = Containerizer.to(registryImage);
        } else {
            containerizer = Containerizer.to(DockerDaemonImage.named(imageReference));
        }
        containerizer.setToolName("Quarkus");
        containerizer.setToolVersion(Version.getVersion());
        containerizer.addEventHandler(LogEvent.class, (e) -> {
            if (!e.getMessage().isEmpty()) {
                log.log(toJBossLoggingLevel(e.getLevel()), e.getMessage());
            }
        });
        containerizer.setAllowInsecureRegistries(containerImageConfig.insecure);
        return containerizer;
    }

    private RegistryImage toRegistryImage(ImageReference imageReference, Optional<String> username, Optional<String> password) {
        CredentialRetrieverFactory credentialRetrieverFactory = CredentialRetrieverFactory.forImage(imageReference,
                log::info);
        RegistryImage registryImage = RegistryImage.named(imageReference);
        registryImage.addCredentialRetriever(credentialRetrieverFactory.wellKnownCredentialHelpers());
        registryImage.addCredentialRetriever(credentialRetrieverFactory.dockerConfig());
        if (username.isPresent() && password.isPresent()) {
            registryImage.addCredential(username.get(), password.get());
        }
        return registryImage;
    }

    private Logger.Level toJBossLoggingLevel(LogEvent.Level level) {
        switch (level) {
            case ERROR:
                return Logger.Level.ERROR;
            case WARN:
                return Logger.Level.WARN;
            case LIFECYCLE:
                return Logger.Level.INFO;
            default:
                return Logger.Level.DEBUG;
        }
    }

    /**
     * We don't use Jib's JavaContainerBuilder here because we need to support the custom fast-jar format
     * We create the following layers (least likely to change to most likely to change):
     *
     * <ul>
     * <li>lib</li>
     * <li>boot-lib</li>
     * <li>quarkus-run.jar</li>
     * <li>quarkus</li>
     * <li>app</li>
     * </ul>
     */
    private JibContainerBuilder createContainerBuilderFromFastJar(JibConfig jibConfig,
            JarBuildItem sourceJarBuildItem,
            List<ContainerImageLabelBuildItem> containerImageLabels) {
        Path componentsPath = sourceJarBuildItem.getPath().getParent();

        AbsoluteUnixPath workDirInContainer = AbsoluteUnixPath.get("/work");

        List<String> entrypoint;
        if (jibConfig.jvmEntrypoint.isPresent()) {
            entrypoint = jibConfig.jvmEntrypoint.get();
        } else {
            entrypoint = new ArrayList<>(3 + jibConfig.jvmArguments.size());
            entrypoint.add("java");
            entrypoint.addAll(jibConfig.jvmArguments);
            entrypoint.add("-jar");
            entrypoint.add(JarResultBuildStep.QUARKUS_RUN_JAR);
        }

        try {
            JibContainerBuilder jibContainerBuilder = Jib
                    .from(toRegistryImage(ImageReference.parse(jibConfig.baseJvmImage), jibConfig.baseRegistryUsername,
                            jibConfig.baseRegistryPassword))
                    .addLayer(Collections.singletonList(componentsPath.resolve(JarResultBuildStep.LIB)), workDirInContainer)
                    .addLayer(Collections.singletonList(componentsPath.resolve(JarResultBuildStep.QUARKUS_RUN_JAR)),
                            workDirInContainer)
                    .addLayer(Collections.singletonList(componentsPath.resolve(JarResultBuildStep.APP)), workDirInContainer)
                    .addLayer(Collections.singletonList(componentsPath.resolve(JarResultBuildStep.QUARKUS)), workDirInContainer)
                    .setWorkingDirectory(workDirInContainer)
                    .setEntrypoint(entrypoint)
                    .setEnvironment(jibConfig.environmentVariables)
                    .setLabels(allLabels(jibConfig, containerImageLabels))
                    .setCreationTime(Instant.now());
            for (int port : jibConfig.ports) {
                jibContainerBuilder.addExposedPort(Port.tcp(port));
            }
            return jibContainerBuilder;

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InvalidImageReferenceException e) {
            throw new RuntimeException(e);
        }
    }

    private void setUser(JibConfig jibConfig, JibContainerBuilder jibContainerBuilder) {
        jibConfig.user.ifPresent(jibContainerBuilder::setUser);
    }

    private JibContainerBuilder createContainerBuilderFromLegacyJar(JibConfig jibConfig,
            JarBuildItem sourceJarBuildItem,
            OutputTargetBuildItem outputTargetBuildItem, MainClassBuildItem mainClassBuildItem,
            List<ContainerImageLabelBuildItem> containerImageLabels) {
        try {
            // not ideal since this has been previously zipped - we would like to just reuse it
            Path classesDir = outputTargetBuildItem.getOutputDirectory().resolve("jib");
            ZipUtils.unzip(sourceJarBuildItem.getPath(), classesDir);
            JavaContainerBuilder javaContainerBuilder = JavaContainerBuilder
                    .from(toRegistryImage(ImageReference.parse(jibConfig.baseJvmImage), jibConfig.baseRegistryUsername,
                            jibConfig.baseRegistryPassword))
                    .addResources(classesDir, IS_CLASS_PREDICATE.negate())
                    .addClasses(classesDir, IS_CLASS_PREDICATE);

            // when there is no custom entry point, we just set everything up for a regular java run
            if (!jibConfig.jvmEntrypoint.isPresent()) {
                javaContainerBuilder
                        .addJvmFlags(jibConfig.jvmArguments)
                        .setMainClass(mainClassBuildItem.getClassName());
            }

            if (sourceJarBuildItem.getLibraryDir() != null) {
                javaContainerBuilder
                        .addDependencies(
                                Files.list(sourceJarBuildItem.getLibraryDir())
                                        .filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".jar"))
                                        .sorted(Comparator.comparing(Path::getFileName))
                                        .collect(Collectors.toList()));
            }

            JibContainerBuilder jibContainerBuilder = javaContainerBuilder.toContainerBuilder()
                    .setEnvironment(jibConfig.environmentVariables)
                    .setLabels(allLabels(jibConfig, containerImageLabels))
                    .setCreationTime(Instant.now());

            if (jibConfig.jvmEntrypoint.isPresent()) {
                jibContainerBuilder.setEntrypoint(jibConfig.jvmEntrypoint.get());
            }

            return jibContainerBuilder;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InvalidImageReferenceException e) {
            throw new RuntimeException(e);
        }
    }

    private JibContainerBuilder createContainerBuilderFromNative(ContainerImageConfig containerImageConfig, JibConfig jibConfig,
            NativeImageBuildItem nativeImageBuildItem, List<ContainerImageLabelBuildItem> containerImageLabels) {

        List<String> entrypoint;
        if (jibConfig.nativeEntrypoint.isPresent()) {
            entrypoint = jibConfig.nativeEntrypoint.get();
        } else {
            List<String> nativeArguments = jibConfig.nativeArguments.orElse(Collections.emptyList());
            entrypoint = new ArrayList<>(nativeArguments.size() + 1);
            entrypoint.add("./" + BINARY_NAME_IN_CONTAINER);
            entrypoint.addAll(nativeArguments);
        }
        try {
            AbsoluteUnixPath workDirInContainer = AbsoluteUnixPath.get("/work");
            JibContainerBuilder jibContainerBuilder = Jib
                    .from(toRegistryImage(ImageReference.parse(jibConfig.baseNativeImage), containerImageConfig.username,
                            containerImageConfig.password))
                    .addFileEntriesLayer(FileEntriesLayer.builder()
                            .addEntry(nativeImageBuildItem.getPath(), workDirInContainer.resolve(BINARY_NAME_IN_CONTAINER),
                                    FilePermissions.fromOctalString("775"))
                            .build())
                    .setWorkingDirectory(workDirInContainer)
                    .setEntrypoint(entrypoint)
                    .setEnvironment(jibConfig.environmentVariables)
                    .setLabels(allLabels(jibConfig, containerImageLabels))
                    .setCreationTime(Instant.now());
            for (int port : jibConfig.ports) {
                jibContainerBuilder.addExposedPort(Port.tcp(port));
            }
            return jibContainerBuilder;
        } catch (InvalidImageReferenceException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Allow users to have custom files in {@code src/main/jib} that will be copied into the built container's file system
     * in same manner as the Jib Maven and Gradle plugins do.
     * For example, {@code src/main/jib/foo/bar} would add {@code /foo/bar} into the container filesystem.
     *
     * See: https://github.com/GoogleContainerTools/jib/blob/v0.15.0-core/docs/faq.md#can-i-add-a-custom-directory-to-the-image
     */
    private void handleExtraFiles(OutputTargetBuildItem outputTarget, JibContainerBuilder jibContainerBuilder) {
        Path outputDirectory = outputTarget.getOutputDirectory();
        PathsUtil.findMainSourcesRoot(outputTarget.getOutputDirectory());
        Map.Entry<Path, Path> mainSourcesRoot = findMainSourcesRoot(outputDirectory);
        if (mainSourcesRoot == null) { // this should never happen
            return;
        }
        Path jibFilesRoot = mainSourcesRoot.getKey().resolve("jib");
        if (!jibFilesRoot.toFile().exists()) {
            return;
        }

        FileEntriesLayer extraFilesLayer;
        try {
            extraFilesLayer = ContainerBuilderHelper.extraDirectoryLayerConfiguration(
                    jibFilesRoot,
                    AbsoluteUnixPath.get("/"),
                    Collections.emptyMap(),
                    (localPath, ignored2) -> {
                        try {
                            return Files.getLastModifiedTime(localPath).toInstant();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
            jibContainerBuilder.addFileEntriesLayer(
                    extraFilesLayer);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Unable to add extra files in '" + jibFilesRoot.toAbsolutePath().toString() + "' to the container", e);
        }
    }

    private Map<String, String> allLabels(JibConfig jibConfig, List<ContainerImageLabelBuildItem> containerImageLabels) {
        if (jibConfig.labels.isEmpty() && containerImageLabels.isEmpty()) {
            return Collections.emptyMap();
        }

        final Map<String, String> allLabels = new HashMap<>(jibConfig.labels);
        for (ContainerImageLabelBuildItem containerImageLabel : containerImageLabels) {
            // we want the user supplied labels to take precedence so the user can override labels generated from other extensions if desired
            allLabels.putIfAbsent(containerImageLabel.getName(), containerImageLabel.getValue());
        }
        return allLabels;
    }

    // TODO: this predicate is rather simplistic since it results in creating the directory structure in both the resources and classes so it should probably be improved to remove empty directories
    private static class IsClassPredicate implements Predicate<Path> {

        @Override
        public boolean test(Path path) {
            return path.getFileName().toString().endsWith(".class");
        }
    }
}
