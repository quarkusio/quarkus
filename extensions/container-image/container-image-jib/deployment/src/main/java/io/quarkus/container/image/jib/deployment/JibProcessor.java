package io.quarkus.container.image.jib.deployment;

import static io.quarkus.container.util.PathsUtil.findMainSourcesRoot;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.util.ZipUtils;
import io.quarkus.builder.Version;
import io.quarkus.container.image.deployment.ContainerImageConfig;
import io.quarkus.container.image.deployment.util.NativeBinaryUtil;
import io.quarkus.container.spi.AvailableContainerImageExtensionBuildItem;
import io.quarkus.container.spi.ContainerImageBuildRequestBuildItem;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImageLabelBuildItem;
import io.quarkus.container.spi.ContainerImagePushRequestBuildItem;
import io.quarkus.container.util.PathsUtil;
import io.quarkus.deployment.IsNormalNotRemoteDev;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.MainClassBuildItem;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.AppCDSContainerImageBuildItem;
import io.quarkus.deployment.pkg.builditem.AppCDSResultBuildItem;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
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

    @BuildStep
    public AvailableContainerImageExtensionBuildItem availability() {
        return new AvailableContainerImageExtensionBuildItem(JIB);
    }

    // when AppCDS are enabled and a container image build via Jib has been requested,
    // we want the AppCDS generation process to use the same JVM as the base image
    // in order to make the AppCDS usable by the runtime JVM
    @BuildStep(onlyIf = JibBuild.class)
    public void appCDS(ContainerImageConfig containerImageConfig, JibConfig jibConfig,
            BuildProducer<AppCDSContainerImageBuildItem> producer) {

        if (!containerImageConfig.build && !containerImageConfig.push) {
            return;
        }

        producer.produce(new AppCDSContainerImageBuildItem(jibConfig.baseJvmImage));
    }

    @BuildStep(onlyIf = { IsNormalNotRemoteDev.class, JibBuild.class }, onlyIfNot = NativeBuild.class)
    public void buildFromJar(ContainerImageConfig containerImageConfig, JibConfig jibConfig,
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

        Boolean buildContainerImage = containerImageConfig.build || buildRequest.isPresent();
        Boolean pushContainerImage = containerImageConfig.push || pushRequest.isPresent();
        if (!buildContainerImage && !pushContainerImage) {
            return;
        }

        JibContainerBuilder jibContainerBuilder;
        String packageType = packageConfig.type;
        if (packageConfig.isLegacyJar() || packageType.equalsIgnoreCase(PackageConfig.UBER_JAR)) {
            jibContainerBuilder = createContainerBuilderFromLegacyJar(jibConfig,
                    sourceJar, outputTarget, mainClass, containerImageLabels);
        } else if (packageConfig.isFastJar()) {
            jibContainerBuilder = createContainerBuilderFromFastJar(jibConfig, sourceJar, curateOutcome, containerImageLabels,
                    appCDSResult);
        } else {
            throw new IllegalArgumentException(
                    "Package type '" + packageType + "' is not supported by the container-image-jib extension");
        }
        setUser(jibConfig, jibContainerBuilder);
        setPlatforms(jibConfig, jibContainerBuilder);
        handleExtraFiles(outputTarget, jibContainerBuilder);
        JibContainer container = containerize(containerImageConfig, jibConfig, containerImage, jibContainerBuilder,
                pushRequest.isPresent());
        writeOutputFiles(container, jibConfig, outputTarget);

        artifactResultProducer.produce(new ArtifactResultBuildItem(null, "jar-container",
                Map.of("container-image", container.getTargetImage().toString(), "pull-required",
                        pushContainerImage.toString())));
    }

    @BuildStep(onlyIf = { IsNormalNotRemoteDev.class, JibBuild.class, NativeBuild.class })
    public void buildFromNative(ContainerImageConfig containerImageConfig, JibConfig jibConfig,
            ContainerImageInfoBuildItem containerImage,
            NativeImageBuildItem nativeImage,
            OutputTargetBuildItem outputTarget,
            Optional<ContainerImageBuildRequestBuildItem> buildRequest,
            Optional<ContainerImagePushRequestBuildItem> pushRequest,
            List<ContainerImageLabelBuildItem> containerImageLabels,
            BuildProducer<ArtifactResultBuildItem> artifactResultProducer) {

        Boolean buildContainerImage = containerImageConfig.build || buildRequest.isPresent();
        Boolean pushContainerImage = containerImageConfig.push || pushRequest.isPresent();
        if (!buildContainerImage && !pushContainerImage) {
            return;
        }

        if (!NativeBinaryUtil.nativeIsLinuxBinary(nativeImage)) {
            throw new RuntimeException(
                    "The native binary produced by the build is not a Linux binary and therefore cannot be used in a Linux container image. Consider adding \"quarkus.native.container-build=true\" to your configuration");
        }

        JibContainerBuilder jibContainerBuilder = createContainerBuilderFromNative(jibConfig,
                nativeImage, containerImageLabels);
        setUser(jibConfig, jibContainerBuilder);
        setPlatforms(jibConfig, jibContainerBuilder);
        handleExtraFiles(outputTarget, jibContainerBuilder);
        JibContainer container = containerize(containerImageConfig, jibConfig, containerImage, jibContainerBuilder,
                pushRequest.isPresent());
        writeOutputFiles(container, jibConfig, outputTarget);

        artifactResultProducer.produce(new ArtifactResultBuildItem(null, "native-container",
                Map.of("container-image", container.getTargetImage().toString(), "pull-required",
                        pushContainerImage.toString())));
    }

    private JibContainer containerize(ContainerImageConfig containerImageConfig,
            JibConfig jibConfig, ContainerImageInfoBuildItem containerImage, JibContainerBuilder jibContainerBuilder,
            boolean pushRequested) {
        Containerizer containerizer = createContainerizer(containerImageConfig, jibConfig, containerImage, pushRequested);
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
            JibConfig jibConfig, ContainerImageInfoBuildItem containerImage,
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
        containerizer.setAlwaysCacheBaseImage(jibConfig.alwaysCacheBaseImage);
        containerizer.setOfflineMode(jibConfig.offlineMode);
        return containerizer;
    }

    private void writeOutputFiles(JibContainer jibContainer, JibConfig jibConfig, OutputTargetBuildItem outputTarget) {
        doWriteOutputFile(outputTarget, Paths.get(jibConfig.imageDigestFile), jibContainer.getDigest().toString());
        doWriteOutputFile(outputTarget, Paths.get(jibConfig.imageIdFile), jibContainer.getImageId().toString());
    }

    private void doWriteOutputFile(OutputTargetBuildItem outputTarget, Path configPath, String output) {
        if (!configPath.isAbsolute()) {
            configPath = outputTarget.getOutputDirectory().resolve(configPath);
        }
        try {
            Files.write(configPath, output.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.errorf(e, "Unable to write file '%s'.", configPath.toAbsolutePath().toString());
        }
    }

    private RegistryImage toRegistryImage(ImageReference imageReference, Optional<String> username, Optional<String> password) {
        CredentialRetrieverFactory credentialRetrieverFactory = CredentialRetrieverFactory.forImage(imageReference,
                log::info);
        RegistryImage registryImage = RegistryImage.named(imageReference);
        if (username.isPresent() && password.isPresent()) {
            registryImage.addCredential(username.get(), password.get());
        } else {
            registryImage.addCredentialRetriever(credentialRetrieverFactory.wellKnownCredentialHelpers());
            registryImage.addCredentialRetriever(credentialRetrieverFactory.dockerConfig());
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
            CurateOutcomeBuildItem curateOutcome, List<ContainerImageLabelBuildItem> containerImageLabels,
            Optional<AppCDSResultBuildItem> appCDSResult) {
        Path componentsPath = sourceJarBuildItem.getPath().getParent();
        Path appLibDir = componentsPath.resolve(JarResultBuildStep.LIB).resolve(JarResultBuildStep.MAIN);

        AbsoluteUnixPath workDirInContainer = AbsoluteUnixPath.get("/work");

        List<String> entrypoint;
        if (jibConfig.jvmEntrypoint.isPresent()) {
            entrypoint = jibConfig.jvmEntrypoint.get();
        } else {
            List<String> effectiveJvmArguments = determineEffectiveJvmArguments(jibConfig, appCDSResult);
            entrypoint = new ArrayList<>(3 + effectiveJvmArguments.size());
            entrypoint.add("java");
            entrypoint.addAll(effectiveJvmArguments);
            entrypoint.add("-jar");
            entrypoint.add(JarResultBuildStep.QUARKUS_RUN_JAR);
        }

        List<AppArtifact> fastChangingLibs = new ArrayList<>();
        List<AppDependency> userDependencies = curateOutcome.getEffectiveModel().getUserDependencies();
        for (AppDependency dep : userDependencies) {
            AppArtifact artifact = dep.getArtifact();
            if (artifact == null) {
                continue;
            }
            String artifactVersion = artifact.getVersion();
            if ((artifactVersion == null) || artifactVersion.isEmpty()) {
                continue;
            }
            if (artifactVersion.toLowerCase().contains("snapshot")) {
                fastChangingLibs.add(artifact);
            }
        }
        Set<Path> fastChangingLibPaths = Collections.emptySet();
        List<Path> nonFastChangingLibPaths = null;
        if (!fastChangingLibs.isEmpty()) {
            fastChangingLibPaths = new HashSet<>(fastChangingLibs.size());
            Map<String, Path> libNameToPath = new HashMap<>();
            try (DirectoryStream<Path> allLibPaths = Files.newDirectoryStream(appLibDir)) {
                for (Path libPath : allLibPaths) {
                    libNameToPath.put(libPath.getFileName().toString(), libPath);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            List<String> libFileNames = new ArrayList<>(libNameToPath.keySet());
            for (AppArtifact appArtifact : fastChangingLibs) {
                String matchingLibDirFileName = null;
                for (Path appArtifactPath : appArtifact.getPaths()) {
                    for (String libFileName : libFileNames) {
                        if (libFileName.contains(appArtifact.getGroupId())
                                && libFileName.contains(appArtifactPath.getFileName().toString())) {
                            matchingLibDirFileName = libFileName;
                            break;
                        }
                    }
                    if (matchingLibDirFileName != null) {
                        break;
                    }
                }
                if (matchingLibDirFileName != null) {
                    fastChangingLibPaths.add(libNameToPath.get(matchingLibDirFileName));
                }
            }
            Collection<Path> allLibPaths = libNameToPath.values();
            nonFastChangingLibPaths = new ArrayList<>(allLibPaths.size() - fastChangingLibPaths.size());
            for (Path libPath : allLibPaths) {
                if (!fastChangingLibPaths.contains(libPath)) {
                    nonFastChangingLibPaths.add(libPath);
                }
            }
        }

        try {

            JibContainerBuilder jibContainerBuilder = Jib
                    .from(toRegistryImage(ImageReference.parse(jibConfig.baseJvmImage), jibConfig.baseRegistryUsername,
                            jibConfig.baseRegistryPassword));
            if (fastChangingLibPaths.isEmpty()) {
                // just create a layer with the entire lib structure intact
                jibContainerBuilder.addLayer(Collections.singletonList(componentsPath.resolve(JarResultBuildStep.LIB)),
                        workDirInContainer);
            } else {
                // we need to manually create each layer
                // the idea here is that the fast changing libraries are created in a later layer, thus when they do change,
                // docker doesn't have to create an entire layer with all dependencies - only change the fast ones

                FileEntriesLayer.Builder bootLibsLayerBuilder = FileEntriesLayer.builder();
                Path bootLibPath = componentsPath.resolve(JarResultBuildStep.LIB).resolve(JarResultBuildStep.BOOT_LIB);
                Files.list(bootLibPath).forEach(lib -> {
                    try {
                        AbsoluteUnixPath libPathInContainer = workDirInContainer.resolve(JarResultBuildStep.LIB)
                                .resolve(JarResultBuildStep.BOOT_LIB)
                                .resolve(lib.getFileName());
                        if (appCDSResult.isPresent()) {
                            // the boot lib jars need to preserve the modification time because otherwise AppCDS won't work
                            bootLibsLayerBuilder.addEntry(lib, libPathInContainer, Files.getLastModifiedTime(lib).toInstant());
                        } else {
                            bootLibsLayerBuilder.addEntry(lib, libPathInContainer);
                        }

                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
                jibContainerBuilder.addFileEntriesLayer(bootLibsLayerBuilder.build());

                Path deploymentPath = componentsPath.resolve(JarResultBuildStep.LIB).resolve(JarResultBuildStep.DEPLOYMENT_LIB);
                if (Files.exists(deploymentPath)) { // this is the case of mutable-jar
                    FileEntriesLayer.Builder deploymentLayerBuilder = FileEntriesLayer.builder();
                    Files.list(deploymentPath).forEach(lib -> {
                        AbsoluteUnixPath libPathInContainer = workDirInContainer.resolve(JarResultBuildStep.LIB)
                                .resolve(JarResultBuildStep.DEPLOYMENT_LIB)
                                .resolve(lib.getFileName());
                        deploymentLayerBuilder.addEntry(lib, libPathInContainer);
                    });
                    jibContainerBuilder.addFileEntriesLayer(deploymentLayerBuilder.build());
                }

                jibContainerBuilder.addLayer(nonFastChangingLibPaths,
                        workDirInContainer.resolve(JarResultBuildStep.LIB).resolve(JarResultBuildStep.MAIN));
                jibContainerBuilder.addLayer(new ArrayList<>(fastChangingLibPaths),
                        workDirInContainer.resolve(JarResultBuildStep.LIB).resolve(JarResultBuildStep.MAIN));
            }

            if (appCDSResult.isPresent()) {
                jibContainerBuilder.addFileEntriesLayer(FileEntriesLayer.builder().addEntry(
                        componentsPath.resolve(JarResultBuildStep.QUARKUS_RUN_JAR),
                        workDirInContainer.resolve(JarResultBuildStep.QUARKUS_RUN_JAR),
                        Files.getLastModifiedTime(componentsPath.resolve(JarResultBuildStep.QUARKUS_RUN_JAR)).toInstant())
                        .build());
                jibContainerBuilder
                        .addLayer(Collections.singletonList(appCDSResult.get().getAppCDS()), workDirInContainer);
            } else {
                jibContainerBuilder.addFileEntriesLayer(FileEntriesLayer.builder().addEntry(
                        componentsPath.resolve(JarResultBuildStep.QUARKUS_RUN_JAR),
                        workDirInContainer.resolve(JarResultBuildStep.QUARKUS_RUN_JAR)).build());
            }

            jibContainerBuilder
                    .addLayer(Collections.singletonList(componentsPath.resolve(JarResultBuildStep.APP)), workDirInContainer)
                    .addLayer(Collections.singletonList(componentsPath.resolve(JarResultBuildStep.QUARKUS)), workDirInContainer)
                    .setWorkingDirectory(workDirInContainer)
                    .setEntrypoint(entrypoint)
                    .setEnvironment(getEnvironmentVariables(jibConfig))
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

    private List<String> determineEffectiveJvmArguments(JibConfig jibConfig, Optional<AppCDSResultBuildItem> appCDSResult) {
        List<String> effectiveJvmArguments = new ArrayList<>(jibConfig.jvmArguments);
        if (appCDSResult.isPresent()) {
            boolean containsAppCDSOptions = false;
            for (String effectiveJvmArgument : effectiveJvmArguments) {
                if (effectiveJvmArgument.startsWith("-XX:SharedArchiveFile")) {
                    containsAppCDSOptions = true;
                    break;
                }
            }
            if (!containsAppCDSOptions) {
                effectiveJvmArguments.add("-XX:SharedArchiveFile=" + appCDSResult.get().getAppCDS().getFileName().toString());
            }
        }
        return effectiveJvmArguments;
    }

    private void setUser(JibConfig jibConfig, JibContainerBuilder jibContainerBuilder) {
        jibConfig.user.ifPresent(jibContainerBuilder::setUser);
    }

    private void setPlatforms(JibConfig jibConfig, JibContainerBuilder jibContainerBuilder) {
        jibConfig.platforms.map(PlatformHelper::parse).ifPresent(jibContainerBuilder::setPlatforms);
    }

    private JibContainerBuilder createContainerBuilderFromLegacyJar(JibConfig jibConfig,
            JarBuildItem sourceJarBuildItem,
            OutputTargetBuildItem outputTargetBuildItem,
            MainClassBuildItem mainClassBuildItem,
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
                try (Stream<Path> dependenciesPaths = Files.list(sourceJarBuildItem.getLibraryDir())) {
                    javaContainerBuilder
                            .addDependencies(
                                    dependenciesPaths
                                            .filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".jar"))
                                            .sorted(Comparator.comparing(Path::getFileName))
                                            .collect(Collectors.toList()));
                }
            }

            JibContainerBuilder jibContainerBuilder = javaContainerBuilder.toContainerBuilder()
                    .setEnvironment(getEnvironmentVariables(jibConfig))
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

    private JibContainerBuilder createContainerBuilderFromNative(JibConfig jibConfig,
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
                    .from(toRegistryImage(ImageReference.parse(jibConfig.baseNativeImage), jibConfig.baseRegistryUsername,
                            jibConfig.baseRegistryPassword))
                    .addFileEntriesLayer(FileEntriesLayer.builder()
                            .addEntry(nativeImageBuildItem.getPath(), workDirInContainer.resolve(BINARY_NAME_IN_CONTAINER),
                                    FilePermissions.fromOctalString("775"))
                            .build())
                    .setWorkingDirectory(workDirInContainer)
                    .setEntrypoint(entrypoint)
                    .setEnvironment(getEnvironmentVariables(jibConfig))
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

    private Map<String, String> getEnvironmentVariables(JibConfig jibConfig) {
        Map<String, String> original = jibConfig.environmentVariables;
        if (original.isEmpty()) {
            return original;
        }
        Map<String, String> converted = new HashMap<>();
        for (Map.Entry<String, String> entry : original.entrySet()) {
            converted.put(entry.getKey().toUpperCase().replace('-', '_').replace('.', '_').replace('/', '_'), entry.getValue());
        }
        return converted;
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
