package io.quarkus.container.image.jib.deployment;

import static com.google.cloud.tools.jib.api.buildplan.FileEntriesLayer.DEFAULT_FILE_PERMISSIONS_PROVIDER;
import static com.google.cloud.tools.jib.api.buildplan.FileEntriesLayer.DEFAULT_OWNERSHIP_PROVIDER;
import static com.google.cloud.tools.jib.api.buildplan.FilePermissions.DEFAULT_FILE_PERMISSIONS;
import static io.quarkus.container.image.deployment.util.EnablementUtil.buildContainerImageNeeded;
import static io.quarkus.container.image.deployment.util.EnablementUtil.pushContainerImageNeeded;
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
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import com.google.cloud.tools.jib.api.CacheDirectoryCreationException;
import com.google.cloud.tools.jib.api.Containerizer;
import com.google.cloud.tools.jib.api.DockerDaemonImage;
import com.google.cloud.tools.jib.api.ImageReference;
import com.google.cloud.tools.jib.api.InvalidImageReferenceException;
import com.google.cloud.tools.jib.api.JavaContainerBuilder;
import com.google.cloud.tools.jib.api.Jib;
import com.google.cloud.tools.jib.api.JibContainer;
import com.google.cloud.tools.jib.api.JibContainerBuilder;
import com.google.cloud.tools.jib.api.LogEvent;
import com.google.cloud.tools.jib.api.RegistryException;
import com.google.cloud.tools.jib.api.RegistryImage;
import com.google.cloud.tools.jib.api.buildplan.AbsoluteUnixPath;
import com.google.cloud.tools.jib.api.buildplan.FileEntriesLayer;
import com.google.cloud.tools.jib.api.buildplan.FileEntry;
import com.google.cloud.tools.jib.api.buildplan.FilePermissions;
import com.google.cloud.tools.jib.api.buildplan.FilePermissionsProvider;
import com.google.cloud.tools.jib.api.buildplan.OwnershipProvider;
import com.google.cloud.tools.jib.api.buildplan.Port;
import com.google.cloud.tools.jib.frontend.CredentialRetrieverFactory;

import io.quarkus.builder.Version;
import io.quarkus.container.image.deployment.ContainerImageConfig;
import io.quarkus.container.image.deployment.util.NativeBinaryUtil;
import io.quarkus.container.spi.AvailableContainerImageExtensionBuildItem;
import io.quarkus.container.spi.ContainerImageBuildRequestBuildItem;
import io.quarkus.container.spi.ContainerImageBuilderBuildItem;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImageLabelBuildItem;
import io.quarkus.container.spi.ContainerImagePushRequestBuildItem;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.MainClassBuildItem;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.AppCDSContainerImageBuildItem;
import io.quarkus.deployment.pkg.builditem.AppCDSResultBuildItem;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.CompiledJavaVersionBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.builditem.JarBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.pkg.builditem.UberJarRequiredBuildItem;
import io.quarkus.deployment.pkg.builditem.UpxCompressedBuildItem;
import io.quarkus.deployment.pkg.steps.JarResultBuildStep;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.deployment.util.ContainerRuntimeUtil;
import io.quarkus.fs.util.ZipUtils;
import io.quarkus.maven.dependency.ResolvedDependency;

public class JibProcessor {

    private static final Logger log = Logger.getLogger(JibProcessor.class);

    public static final String JIB = "jib";
    private static final IsClassPredicate IS_CLASS_PREDICATE = new IsClassPredicate();
    private static final String BINARY_NAME_IN_CONTAINER = "application";

    private static final String UBI8_PREFIX = "registry.access.redhat.com/ubi8";
    private static final String OPENJDK_PREFIX = "openjdk";
    private static final String RUNTIME_SUFFIX = "runtime";

    private static final String JAVA_21_BASE_IMAGE = String.format("%s/%s-21-%s:1.18", UBI8_PREFIX, OPENJDK_PREFIX,
            RUNTIME_SUFFIX);
    private static final String JAVA_17_BASE_IMAGE = String.format("%s/%s-17-%s:1.18", UBI8_PREFIX, OPENJDK_PREFIX,
            RUNTIME_SUFFIX);

    private static final String RUN_JAVA_PATH = "/opt/jboss/container/java/run/run-java.sh";

    private static final String DEFAULT_BASE_IMAGE_USER = "185";

    private static final String OPENTELEMETRY_CONTEXT_CONTEXT_STORAGE_PROVIDER_SYS_PROP = "io.opentelemetry.context.contextStorageProvider";
    private static final FilePermissions REMOTE_DEV_FOLDER_PERMISSIONS = FilePermissions.fromOctalString("777");
    private static final FilePermissions REMOTE_DEV_FILE_PERMISSIONS = FilePermissions.fromOctalString("666");

    private static final FilePermissionsProvider REMOTE_DEV_FOLDER_PERMISSIONS_PROVIDER = (sourcePath,
            destinationPath) -> Files.isDirectory(sourcePath)
                    ? REMOTE_DEV_FOLDER_PERMISSIONS
                    : REMOTE_DEV_FILE_PERMISSIONS;

    private static final OwnershipProvider REMOTE_DEV_OWNERSHIP_PROVIDER = (sourcePath,
            destinationPath) -> DEFAULT_BASE_IMAGE_USER;

    @BuildStep
    public AvailableContainerImageExtensionBuildItem availability() {
        return new AvailableContainerImageExtensionBuildItem(JIB);
    }

    // when AppCDS are enabled and a container image build via Jib has been requested,
    // we want the AppCDS generation process to use the same JVM as the base image
    // in order to make the AppCDS usable by the runtime JVM
    @BuildStep(onlyIf = JibBuild.class)
    public void appCDS(ContainerImageConfig containerImageConfig, CompiledJavaVersionBuildItem compiledJavaVersion,
            ContainerImageJibConfig jibConfig,
            BuildProducer<AppCDSContainerImageBuildItem> producer) {

        if (!containerImageConfig.isBuildExplicitlyEnabled() && !containerImageConfig.isPushExplicitlyEnabled()) {
            return;
        }

        producer.produce(new AppCDSContainerImageBuildItem(determineBaseJvmImage(jibConfig, compiledJavaVersion)));
    }

    private String determineBaseJvmImage(ContainerImageJibConfig jibConfig, CompiledJavaVersionBuildItem compiledJavaVersion) {
        if (jibConfig.baseJvmImage.isPresent()) {
            return jibConfig.baseJvmImage.get();
        }

        var javaVersion = compiledJavaVersion.getJavaVersion();
        if (javaVersion.isJava21OrHigher() == CompiledJavaVersionBuildItem.JavaVersion.Status.TRUE) {
            return JAVA_21_BASE_IMAGE;
        }
        return JAVA_17_BASE_IMAGE;
    }

    @BuildStep(onlyIf = { IsNormal.class, JibBuild.class }, onlyIfNot = NativeBuild.class)
    public void buildFromJar(ContainerImageConfig containerImageConfig, ContainerImageJibConfig jibConfig,
            PackageConfig packageConfig,
            ContainerImageInfoBuildItem containerImage,
            JarBuildItem sourceJar,
            MainClassBuildItem mainClass,
            OutputTargetBuildItem outputTarget,
            CurateOutcomeBuildItem curateOutcome,
            CompiledJavaVersionBuildItem compiledJavaVersion,
            Optional<ContainerImageBuildRequestBuildItem> buildRequest,
            Optional<ContainerImagePushRequestBuildItem> pushRequest,
            List<ContainerImageLabelBuildItem> containerImageLabels,
            Optional<AppCDSResultBuildItem> appCDSResult,
            List<UberJarRequiredBuildItem> uberJarRequired,
            BuildProducer<ArtifactResultBuildItem> artifactResultProducer,
            BuildProducer<ContainerImageBuilderBuildItem> containerImageBuilder) {

        boolean buildContainerImage = buildContainerImageNeeded(containerImageConfig, buildRequest);
        boolean pushContainerImage = pushContainerImageNeeded(containerImageConfig, pushRequest);
        if (!buildContainerImage && !pushContainerImage) {
            return;
        }

        JibContainerBuilder jibContainerBuilder;
        String packageType = packageConfig.type;
        if (packageConfig.isLegacyJar() || packageType.equalsIgnoreCase(PackageConfig.BuiltInType.UBER_JAR.getValue())
                || !uberJarRequired.isEmpty()) {
            jibContainerBuilder = createContainerBuilderFromLegacyJar(determineBaseJvmImage(jibConfig, compiledJavaVersion),
                    jibConfig, containerImageConfig,
                    sourceJar, outputTarget, mainClass, containerImageLabels);
        } else if (packageConfig.isFastJar()) {
            jibContainerBuilder = createContainerBuilderFromFastJar(determineBaseJvmImage(jibConfig, compiledJavaVersion),
                    jibConfig, containerImageConfig, sourceJar, curateOutcome,
                    containerImageLabels,
                    appCDSResult, packageType.equals(PackageConfig.BuiltInType.MUTABLE_JAR.getValue()));
        } else {
            throw new IllegalArgumentException(
                    "Package type '" + packageType + "' is not supported by the container-image-jib extension");
        }
        setUser(jibConfig, jibContainerBuilder);
        setPlatforms(jibConfig, jibContainerBuilder);
        handleExtraFiles(outputTarget, jibContainerBuilder);
        log.info("Starting (local) container image build for jar using jib.");
        JibContainer container = containerize(containerImageConfig, jibConfig, containerImage, jibContainerBuilder,
                pushRequest.isPresent());
        writeOutputFiles(container, jibConfig, outputTarget);

        artifactResultProducer.produce(new ArtifactResultBuildItem(null, "jar-container",
                Map.of("container-image", container.getTargetImage().toString(), "pull-required",
                        Boolean.toString(pushContainerImage))));
        containerImageBuilder.produce(new ContainerImageBuilderBuildItem(JIB));
    }

    @BuildStep(onlyIf = { IsNormal.class, JibBuild.class, NativeBuild.class })
    public void buildFromNative(ContainerImageConfig containerImageConfig, ContainerImageJibConfig jibConfig,
            ContainerImageInfoBuildItem containerImage,
            NativeImageBuildItem nativeImage,
            OutputTargetBuildItem outputTarget,
            Optional<ContainerImageBuildRequestBuildItem> buildRequest,
            Optional<ContainerImagePushRequestBuildItem> pushRequest,
            List<ContainerImageLabelBuildItem> containerImageLabels,
            Optional<UpxCompressedBuildItem> upxCompressed, // used to ensure that we work with the compressed native binary if compression was enabled
            BuildProducer<ArtifactResultBuildItem> artifactResultProducer,
            BuildProducer<ContainerImageBuilderBuildItem> containerImageBuilder) {

        boolean buildContainerImage = buildContainerImageNeeded(containerImageConfig, buildRequest);
        boolean pushContainerImage = pushContainerImageNeeded(containerImageConfig, pushRequest);
        if (!buildContainerImage && !pushContainerImage) {
            return;
        }

        if (!NativeBinaryUtil.nativeIsLinuxBinary(nativeImage)) {
            throw new RuntimeException(
                    "The native binary produced by the build is not a Linux binary and therefore cannot be used in a Linux container image. Consider adding \"quarkus.native.container-build=true\" to your configuration");
        }

        JibContainerBuilder jibContainerBuilder = createContainerBuilderFromNative(jibConfig, containerImageConfig,
                nativeImage, containerImageLabels);
        setUser(jibConfig, jibContainerBuilder);
        setPlatforms(jibConfig, jibContainerBuilder);
        handleExtraFiles(outputTarget, jibContainerBuilder);

        log.info("Starting (local) container image build for native binary using jib.");
        JibContainer container = containerize(containerImageConfig, jibConfig, containerImage, jibContainerBuilder,
                pushContainerImage);
        writeOutputFiles(container, jibConfig, outputTarget);

        artifactResultProducer.produce(new ArtifactResultBuildItem(null, "native-container",
                Map.of("container-image", container.getTargetImage().toString(), "pull-required",
                        "" + pushContainerImage)));
        containerImageBuilder.produce(new ContainerImageBuilderBuildItem(JIB));
    }

    private JibContainer containerize(ContainerImageConfig containerImageConfig,
            ContainerImageJibConfig jibConfig, ContainerImageInfoBuildItem containerImage,
            JibContainerBuilder jibContainerBuilder,
            boolean pushRequested) {

        Containerizer containerizer = createContainerizer(containerImageConfig, jibConfig, containerImage, pushRequested);
        for (String additionalTag : containerImage.getAdditionalTags()) {
            containerizer.withAdditionalTag(additionalTag);
        }
        String previousContextStorageSysProp = null;
        try {
            // Jib uses the Google HTTP Client under the hood which attempts to record traces via OpenCensus which is wired
            // to delegate to OpenTelemetry.
            // This can lead to problems with the Quarkus OpenTelemetry extension which expects Vert.x to be running,
            // something that is not the case at build time, see https://github.com/quarkusio/quarkus/issues/22864.
            previousContextStorageSysProp = System.setProperty(OPENTELEMETRY_CONTEXT_CONTEXT_STORAGE_PROVIDER_SYS_PROP,
                    "default");

            JibContainer container = containerizeUnderLock(jibContainerBuilder, containerizer);
            log.infof("%s container image %s (%s)\n",
                    containerImageConfig.isPushExplicitlyEnabled() ? "Pushed" : "Created",
                    container.getTargetImage(),
                    container.getDigest());
            return container;
        } catch (Exception e) {
            throw new RuntimeException("Unable to create container image", e);
        } finally {
            if (previousContextStorageSysProp == null) {
                System.clearProperty(OPENTELEMETRY_CONTEXT_CONTEXT_STORAGE_PROVIDER_SYS_PROP);
            } else {
                System.setProperty(OPENTELEMETRY_CONTEXT_CONTEXT_STORAGE_PROVIDER_SYS_PROP, previousContextStorageSysProp);
            }
        }
    }

    private Containerizer createContainerizer(ContainerImageConfig containerImageConfig,
            ContainerImageJibConfig jibConfig, ContainerImageInfoBuildItem containerImageInfo,
            boolean pushRequested) {
        Containerizer containerizer;
        ImageReference imageReference = ImageReference.of(containerImageInfo.getRegistry().orElse(null),
                containerImageInfo.getRepository(), containerImageInfo.getTag());

        if (pushRequested || containerImageConfig.isPushExplicitlyEnabled()) {
            if (imageReference.getRegistry() == null) {
                log.info("No container image registry was set, so 'docker.io' will be used");
            }
            RegistryImage registryImage = toRegistryImage(imageReference, containerImageConfig.username,
                    containerImageConfig.password);
            containerizer = Containerizer.to(registryImage);
        } else {
            DockerDaemonImage dockerDaemonImage = DockerDaemonImage.named(imageReference);
            Optional<String> dockerConfigExecutableName = ConfigProvider.getConfig()
                    .getOptionalValue("quarkus.docker.executable-name", String.class);
            Optional<String> jibConfigExecutableName = jibConfig.dockerExecutableName;
            if (jibConfigExecutableName.isPresent()) {
                dockerDaemonImage.setDockerExecutable(Paths.get(jibConfigExecutableName.get()));
            } else if (dockerConfigExecutableName.isPresent()) {
                dockerDaemonImage.setDockerExecutable(Paths.get(dockerConfigExecutableName.get()));
            } else {
                // detect the container runtime instead of falling back to 'docker' as the default
                ContainerRuntimeUtil.ContainerRuntime detectedContainerRuntime = ContainerRuntimeUtil.detectContainerRuntime();
                log.infof("Using %s to run the native image builder", detectedContainerRuntime.getExecutableName());
                dockerDaemonImage.setDockerExecutable(Paths.get(detectedContainerRuntime.getExecutableName()));
            }
            dockerDaemonImage.setDockerEnvironment(jibConfig.dockerEnvironment);
            containerizer = Containerizer.to(dockerDaemonImage);
        }
        containerizer.setToolName("Quarkus");
        containerizer.setToolVersion(Version.getVersion());
        containerizer.addEventHandler(LogEvent.class, e -> {
            if (!e.getMessage().isEmpty()) {
                log.log(toJBossLoggingLevel(e.getLevel()), e.getMessage());
            }
        });
        containerizer.setAllowInsecureRegistries(containerImageConfig.insecure);
        containerizer.setAlwaysCacheBaseImage(jibConfig.alwaysCacheBaseImage);
        containerizer.setOfflineMode(jibConfig.offlineMode);
        jibConfig.baseImageLayersCache.ifPresent(cacheDir -> containerizer.setBaseImageLayersCache(Paths.get(cacheDir)));
        jibConfig.applicationLayersCache.ifPresent(cacheDir -> containerizer.setApplicationLayersCache(Paths.get(cacheDir)));

        return containerizer;
    }

    /**
     * Wraps the containerize invocation in a synchronized block to avoid OverlappingFileLockException when running parallel jib
     * builds (e.g. mvn -T2 ...).
     * Each build thread uses its own augmentation CL (which is why the OverlappingFileLockException prevention in jib doesn't
     * work here), so the lock object
     * has to be loaded via the parent classloader so that all build threads lock the same object.
     * QuarkusAugmentor was chosen semi-randomly (note: quarkus-core-deployment is visible to that parent CL, this jib extension
     * is not!).
     */
    private JibContainer containerizeUnderLock(JibContainerBuilder jibContainerBuilder, Containerizer containerizer)
            throws InterruptedException, RegistryException, IOException, CacheDirectoryCreationException, ExecutionException {
        Class<?> lockObj = getClass();
        ClassLoader parentCL = getClass().getClassLoader().getParent();
        try {
            lockObj = parentCL.loadClass("io.quarkus.deployment.QuarkusAugmentor");
        } catch (ClassNotFoundException e) {
            log.warnf("Could not load io.quarkus.deployment.QuarkusAugmentor with parent classloader: %s", parentCL);
        }
        synchronized (lockObj) {
            return jibContainerBuilder.containerize(containerizer);
        }
    }

    private void writeOutputFiles(JibContainer jibContainer, ContainerImageJibConfig jibConfig,
            OutputTargetBuildItem outputTarget) {
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

    private JibContainerBuilder toJibContainerBuilder(String baseImage, ContainerImageJibConfig jibConfig)
            throws InvalidImageReferenceException {
        if (baseImage.startsWith(Jib.TAR_IMAGE_PREFIX) || baseImage.startsWith(Jib.DOCKER_DAEMON_IMAGE_PREFIX)) {
            return Jib.from(baseImage);
        }
        return Jib.from(toRegistryImage(ImageReference.parse(baseImage), jibConfig.baseRegistryUsername,
                jibConfig.baseRegistryPassword));
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

            // podman credentials: https://docs.podman.io/en/latest/markdown/podman-login.1.html
            // podman for Windows and macOS
            registryImage.addCredentialRetriever(credentialRetrieverFactory
                    .dockerConfig(Paths.get(System.getProperty("user.home"), ".config", "containers", "auth.json")));
            String xdgRuntimeDir = System.getenv("XDG_RUNTIME_DIR");
            if ((xdgRuntimeDir != null) && !xdgRuntimeDir.isEmpty()) {
                registryImage.addCredentialRetriever(
                        credentialRetrieverFactory.dockerConfig(Paths.get(xdgRuntimeDir, "containers", "auth.json")));
            }

            String dockerConfigEnv = System.getenv().get("DOCKER_CONFIG");
            if (dockerConfigEnv != null) {
                Path dockerConfigPath = Path.of(dockerConfigEnv);
                if (Files.isDirectory(dockerConfigPath)) {
                    // this matches jib's behaviour,
                    // see https://github.com/GoogleContainerTools/jib/blob/master/jib-maven-plugin/README.md#authentication-methods
                    dockerConfigPath = dockerConfigPath.resolve("config.json");
                }
                registryImage.addCredentialRetriever(credentialRetrieverFactory.dockerConfig(dockerConfigPath));
            }

            registryImage.addCredentialRetriever(credentialRetrieverFactory.googleApplicationDefaultCredentials());
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
    private JibContainerBuilder createContainerBuilderFromFastJar(String baseJvmImage, ContainerImageJibConfig jibConfig,
            ContainerImageConfig containerImageConfig,
            JarBuildItem sourceJarBuildItem,
            CurateOutcomeBuildItem curateOutcome, List<ContainerImageLabelBuildItem> containerImageLabels,
            Optional<AppCDSResultBuildItem> appCDSResult,
            boolean isMutableJar) {
        Path componentsPath = sourceJarBuildItem.getPath().getParent();
        Path appLibDir = componentsPath.resolve(JarResultBuildStep.LIB).resolve(JarResultBuildStep.MAIN);

        AbsoluteUnixPath workDirInContainer = AbsoluteUnixPath.get(jibConfig.workingDirectory);
        Map<String, String> envVars = createEnvironmentVariables(jibConfig);

        List<String> entrypoint;
        if (jibConfig.jvmEntrypoint.isPresent()) {
            entrypoint = Collections.unmodifiableList(jibConfig.jvmEntrypoint.get());
        } else if (containsRunJava(baseJvmImage) && appCDSResult.isEmpty()) {
            // we want to use run-java.sh by default. However, if AppCDS are being used, run-java.sh cannot be used because it would lead to using different JVM args
            // which would mean AppCDS would not be taken into account at all
            entrypoint = List.of(RUN_JAVA_PATH);
            envVars.put("JAVA_APP_JAR", workDirInContainer + "/" + JarResultBuildStep.QUARKUS_RUN_JAR);
            envVars.put("JAVA_OPTS_APPEND", String.join(" ", determineEffectiveJvmArguments(jibConfig, appCDSResult)));
        } else {
            List<String> effectiveJvmArguments = determineEffectiveJvmArguments(jibConfig, appCDSResult);
            List<String> argsList = new ArrayList<>(3 + effectiveJvmArguments.size());
            argsList.add("java");
            argsList.addAll(effectiveJvmArguments);
            argsList.add("-jar");
            argsList.add(JarResultBuildStep.QUARKUS_RUN_JAR);
            entrypoint = Collections.unmodifiableList(argsList);
        }

        List<ResolvedDependency> fastChangingLibs = new ArrayList<>();
        Collection<ResolvedDependency> userDependencies = curateOutcome.getApplicationModel().getRuntimeDependencies();
        for (ResolvedDependency artifact : userDependencies) {
            if (artifact == null) {
                continue;
            }
            if (artifact.isWorkspaceModule()) {
                fastChangingLibs.add(artifact);
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
            for (ResolvedDependency appArtifact : fastChangingLibs) {
                String matchingLibDirFileName = null;
                for (Path appArtifactPath : appArtifact.getResolvedPaths()) {
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
            Instant now = Instant.now();
            Instant modificationTime = jibConfig.useCurrentTimestampFileModification ? now : Instant.EPOCH;

            JibContainerBuilder jibContainerBuilder = toJibContainerBuilder(baseJvmImage, jibConfig);
            if (fastChangingLibPaths.isEmpty()) {
                // just create a layer with the entire lib structure intact
                addLayer(jibContainerBuilder, Collections.singletonList(componentsPath.resolve(JarResultBuildStep.LIB)),
                        workDirInContainer, "fast-jar-lib", isMutableJar, modificationTime);
            } else {
                // we need to manually create each layer
                // the idea here is that the fast changing libraries are created in a later layer, thus when they do change,
                // docker doesn't have to create an entire layer with all dependencies - only change the fast ones

                FileEntriesLayer.Builder bootLibsLayerBuilder = FileEntriesLayer.builder().setName("fast-jar-boot-libs");
                Path bootLibPath = componentsPath.resolve(JarResultBuildStep.LIB).resolve(JarResultBuildStep.BOOT_LIB);
                try (Stream<Path> boolLibPaths = Files.list(bootLibPath)) {
                    boolLibPaths.forEach(lib -> {
                        try {
                            AbsoluteUnixPath libPathInContainer = workDirInContainer.resolve(JarResultBuildStep.LIB)
                                    .resolve(JarResultBuildStep.BOOT_LIB)
                                    .resolve(lib.getFileName());
                            if (appCDSResult.isPresent()) {
                                // the boot lib jars need to preserve the modification time because otherwise AppCDS won't work
                                bootLibsLayerBuilder.addEntry(lib, libPathInContainer,
                                        Files.getLastModifiedTime(lib).toInstant());
                            } else {
                                bootLibsLayerBuilder.addEntry(lib, libPathInContainer);
                            }

                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
                }
                jibContainerBuilder.addFileEntriesLayer(bootLibsLayerBuilder.build());

                if (isMutableJar) {
                    Path deploymentPath = componentsPath.resolve(JarResultBuildStep.LIB)
                            .resolve(JarResultBuildStep.DEPLOYMENT_LIB);
                    addLayer(jibContainerBuilder, Collections.singletonList(deploymentPath),
                            workDirInContainer.resolve(JarResultBuildStep.LIB),
                            "fast-jar-deployment-libs", true, modificationTime);
                }

                AbsoluteUnixPath libsMainPath = workDirInContainer.resolve(JarResultBuildStep.LIB)
                        .resolve(JarResultBuildStep.MAIN);
                addLayer(jibContainerBuilder, nonFastChangingLibPaths, libsMainPath, "fast-jar-normal-libs",
                        isMutableJar, modificationTime);
                addLayer(jibContainerBuilder, new ArrayList<>(fastChangingLibPaths), libsMainPath, "fast-jar-changing-libs",
                        isMutableJar, modificationTime);
            }

            if (appCDSResult.isPresent()) {
                jibContainerBuilder.addFileEntriesLayer(FileEntriesLayer.builder().setName("app-cds").addEntry(
                        componentsPath.resolve(JarResultBuildStep.QUARKUS_RUN_JAR),
                        workDirInContainer.resolve(JarResultBuildStep.QUARKUS_RUN_JAR),
                        Files.getLastModifiedTime(componentsPath.resolve(JarResultBuildStep.QUARKUS_RUN_JAR)).toInstant())
                        .build());
                jibContainerBuilder
                        .addLayer(Collections.singletonList(appCDSResult.get().getAppCDS()), workDirInContainer);
            } else {
                jibContainerBuilder.addFileEntriesLayer(FileEntriesLayer.builder()
                        .setName("fast-jar-run")
                        .addEntry(
                                componentsPath.resolve(JarResultBuildStep.QUARKUS_RUN_JAR),
                                workDirInContainer.resolve(JarResultBuildStep.QUARKUS_RUN_JAR),
                                isMutableJar ? REMOTE_DEV_FILE_PERMISSIONS : DEFAULT_FILE_PERMISSIONS,
                                modificationTime,
                                isMutableJar ? DEFAULT_BASE_IMAGE_USER : "")
                        .build());
            }

            addLayer(jibContainerBuilder, Collections.singletonList(componentsPath.resolve(JarResultBuildStep.APP)),
                    workDirInContainer, "fast-jar-quarkus-app", isMutableJar, modificationTime);
            addLayer(jibContainerBuilder, Collections.singletonList(componentsPath.resolve(JarResultBuildStep.QUARKUS)),
                    workDirInContainer, "fast-jar-quarkus", isMutableJar, modificationTime);
            if (ContainerImageJibConfig.DEFAULT_WORKING_DIR.equals(jibConfig.workingDirectory)) {
                // this layer ensures that the working directory is writeable
                // see https://github.com/GoogleContainerTools/jib/issues/1270
                // TODO: is this needed for all working directories?
                jibContainerBuilder.addFileEntriesLayer(FileEntriesLayer.builder().addEntry(
                        new FileEntry(
                                Files.createTempDirectory("jib"),
                                AbsoluteUnixPath.get(jibConfig.workingDirectory),
                                FilePermissions.DEFAULT_FOLDER_PERMISSIONS,
                                modificationTime, DEFAULT_BASE_IMAGE_USER))
                        .build());
            }
            if (isMutableJar) {
                // this layer is needed for remote-dev
                jibContainerBuilder.addFileEntriesLayer(FileEntriesLayer.builder()
                        .addEntry(
                                new FileEntry(
                                        Files.createTempDirectory("jib"),
                                        workDirInContainer.resolve("dev"),
                                        REMOTE_DEV_FOLDER_PERMISSIONS,
                                        modificationTime, DEFAULT_BASE_IMAGE_USER))
                        .addEntry(
                                new FileEntry(
                                        componentsPath.resolve(JarResultBuildStep.QUARKUS_APP_DEPS),
                                        workDirInContainer.resolve(JarResultBuildStep.QUARKUS_APP_DEPS),
                                        REMOTE_DEV_FOLDER_PERMISSIONS,
                                        modificationTime, DEFAULT_BASE_IMAGE_USER))
                        .build());
            }

            jibContainerBuilder
                    .setWorkingDirectory(workDirInContainer)
                    .setEntrypoint(entrypoint)
                    .setEnvironment(envVars)
                    .setLabels(allLabels(jibConfig, containerImageConfig, containerImageLabels));

            mayInheritEntrypoint(jibContainerBuilder, entrypoint, jibConfig.jvmArguments);

            if (jibConfig.useCurrentTimestamp) {
                jibContainerBuilder.setCreationTime(now);
            }

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

    // TODO: this needs to be a lot more sophisticated
    private boolean containsRunJava(String baseJvmImage) {
        return baseJvmImage.startsWith(UBI8_PREFIX) && baseJvmImage.contains(OPENJDK_PREFIX)
                && baseJvmImage.contains(RUNTIME_SUFFIX);
    }

    public JibContainerBuilder addLayer(JibContainerBuilder jibContainerBuilder, List<Path> files,
            AbsoluteUnixPath pathInContainer, String name, boolean isMutableJar,
            Instant now)
            throws IOException {
        FileEntriesLayer.Builder layerConfigurationBuilder = FileEntriesLayer.builder().setName(name);

        for (Path file : files) {
            layerConfigurationBuilder.addEntryRecursive(
                    file, pathInContainer.resolve(file.getFileName()),
                    isMutableJar ? REMOTE_DEV_FOLDER_PERMISSIONS_PROVIDER : DEFAULT_FILE_PERMISSIONS_PROVIDER,
                    (sourcePath, destinationPath) -> now,
                    isMutableJar ? REMOTE_DEV_OWNERSHIP_PROVIDER : DEFAULT_OWNERSHIP_PROVIDER);
        }

        return jibContainerBuilder.addFileEntriesLayer(layerConfigurationBuilder.build());
    }

    private void mayInheritEntrypoint(JibContainerBuilder jibContainerBuilder, List<String> entrypoint,
            List<String> arguments) {
        if (entrypoint.size() == 1 && "INHERIT".equals(entrypoint.get(0))) {
            jibContainerBuilder
                    .setEntrypoint((List<String>) null)
                    .setProgramArguments(arguments);
        }
    }

    private List<String> determineEffectiveJvmArguments(ContainerImageJibConfig jibConfig,
            Optional<AppCDSResultBuildItem> appCDSResult) {
        List<String> effectiveJvmArguments = new ArrayList<>(jibConfig.jvmArguments);
        jibConfig.jvmAdditionalArguments.ifPresent(effectiveJvmArguments::addAll);
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

    private void setUser(ContainerImageJibConfig jibConfig, JibContainerBuilder jibContainerBuilder) {
        jibConfig.user.ifPresent(jibContainerBuilder::setUser);
    }

    private void setPlatforms(ContainerImageJibConfig jibConfig, JibContainerBuilder jibContainerBuilder) {
        jibConfig.platforms.map(PlatformHelper::parse).ifPresent(jibContainerBuilder::setPlatforms);
    }

    private JibContainerBuilder createContainerBuilderFromLegacyJar(String baseJvmImage, ContainerImageJibConfig jibConfig,
            ContainerImageConfig containerImageConfig,
            JarBuildItem sourceJarBuildItem,
            OutputTargetBuildItem outputTargetBuildItem,
            MainClassBuildItem mainClassBuildItem,
            List<ContainerImageLabelBuildItem> containerImageLabels) {
        try {
            // not ideal since this has been previously zipped - we would like to just reuse it
            Path classesDir = outputTargetBuildItem.getOutputDirectory().resolve("jib");
            ZipUtils.unzip(sourceJarBuildItem.getPath(), classesDir);

            JavaContainerBuilder javaContainerBuilder;
            if (baseJvmImage.startsWith(Jib.TAR_IMAGE_PREFIX) || baseJvmImage.startsWith(Jib.DOCKER_DAEMON_IMAGE_PREFIX)) {
                javaContainerBuilder = JavaContainerBuilder.from(baseJvmImage);
            } else {
                javaContainerBuilder = JavaContainerBuilder
                        .from(toRegistryImage(ImageReference.parse(baseJvmImage), jibConfig.baseRegistryUsername,
                                jibConfig.baseRegistryPassword));
            }

            javaContainerBuilder = javaContainerBuilder
                    .addResources(classesDir, IS_CLASS_PREDICATE.negate())
                    .addClasses(classesDir, IS_CLASS_PREDICATE);

            // when there is no custom entry point, we just set everything up for a regular java run
            if (!jibConfig.jvmEntrypoint.isPresent()) {
                javaContainerBuilder
                        .addJvmFlags(determineEffectiveJvmArguments(jibConfig, Optional.empty()))
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
                    .setEnvironment(createEnvironmentVariables(jibConfig))
                    .setLabels(allLabels(jibConfig, containerImageConfig, containerImageLabels));

            if (jibConfig.useCurrentTimestamp) {
                jibContainerBuilder.setCreationTime(Instant.now());
            }

            if (jibConfig.jvmEntrypoint.isPresent()) {
                jibContainerBuilder.setEntrypoint(jibConfig.jvmEntrypoint.get());
                mayInheritEntrypoint(jibContainerBuilder, jibConfig.jvmEntrypoint.get(), jibConfig.jvmArguments);
            }

            return jibContainerBuilder;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InvalidImageReferenceException e) {
            throw new RuntimeException(e);
        }
    }

    private JibContainerBuilder createContainerBuilderFromNative(ContainerImageJibConfig jibConfig,
            ContainerImageConfig containerImageConfig,
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
            JibContainerBuilder jibContainerBuilder = toJibContainerBuilder(jibConfig.baseNativeImage, jibConfig)
                    .addFileEntriesLayer(FileEntriesLayer.builder()
                            .addEntry(nativeImageBuildItem.getPath(), workDirInContainer.resolve(BINARY_NAME_IN_CONTAINER),
                                    FilePermissions.fromOctalString("775"))
                            .build())
                    .setWorkingDirectory(workDirInContainer)
                    .setEntrypoint(entrypoint)
                    .setEnvironment(createEnvironmentVariables(jibConfig))
                    .setLabels(allLabels(jibConfig, containerImageConfig, containerImageLabels));

            mayInheritEntrypoint(jibContainerBuilder, entrypoint, jibConfig.nativeArguments.orElse(null));

            if (jibConfig.useCurrentTimestamp) {
                jibContainerBuilder.setCreationTime(Instant.now());
            }

            for (int port : jibConfig.ports) {
                jibContainerBuilder.addExposedPort(Port.tcp(port));
            }
            return jibContainerBuilder;
        } catch (InvalidImageReferenceException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, String> createEnvironmentVariables(ContainerImageJibConfig jibConfig) {
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

    private Map<String, String> allLabels(ContainerImageJibConfig jibConfig, ContainerImageConfig containerImageConfig,
            List<ContainerImageLabelBuildItem> containerImageLabels) {
        if (containerImageLabels.isEmpty() && containerImageConfig.labels.isEmpty()) {
            return Collections.emptyMap();
        }

        final Map<String, String> allLabels = new HashMap<>(containerImageConfig.labels);
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
