package io.quarkus.container.image.jib.deployment;

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

import com.google.cloud.tools.jib.api.AbsoluteUnixPath;
import com.google.cloud.tools.jib.api.Containerizer;
import com.google.cloud.tools.jib.api.DockerDaemonImage;
import com.google.cloud.tools.jib.api.FilePermissions;
import com.google.cloud.tools.jib.api.ImageReference;
import com.google.cloud.tools.jib.api.InvalidImageReferenceException;
import com.google.cloud.tools.jib.api.JavaContainerBuilder;
import com.google.cloud.tools.jib.api.Jib;
import com.google.cloud.tools.jib.api.JibContainer;
import com.google.cloud.tools.jib.api.JibContainerBuilder;
import com.google.cloud.tools.jib.api.LayerConfiguration;
import com.google.cloud.tools.jib.api.LogEvent;
import com.google.cloud.tools.jib.api.RegistryImage;
import com.google.cloud.tools.jib.frontend.CredentialRetrieverFactory;

import io.quarkus.bootstrap.util.ZipUtils;
import io.quarkus.container.image.deployment.ContainerImageConfig;
import io.quarkus.container.image.deployment.util.NativeBinaryUtil;
import io.quarkus.container.spi.ContainerImageBuildRequestBuildItem;
import io.quarkus.container.spi.ContainerImageLabelBuildItem;
import io.quarkus.container.spi.ContainerImagePushRequestBuildItem;
import io.quarkus.container.spi.ContainerImageResultBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.MainClassBuildItem;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.JarBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;

public class JibProcessor {

    private static final Logger log = Logger.getLogger(JibProcessor.class);

    private static final String JIB = "jib";
    private static final IsClassPredicate IS_CLASS_PREDICATE = new IsClassPredicate();
    private static final String BINARY_NAME_IN_CONTAINER = "application";

    @BuildStep
    public CapabilityBuildItem capability() {
        return new CapabilityBuildItem(Capabilities.CONTAINER_IMAGE_JIB);
    }

    @BuildStep(onlyIf = { IsNormal.class, JibBuild.class }, onlyIfNot = NativeBuild.class)
    public void buildFromJar(ContainerImageConfig containerImageConfig, JibConfig jibConfig,
            JarBuildItem sourceJar,
            MainClassBuildItem mainClass,
            OutputTargetBuildItem outputTarget, ApplicationInfoBuildItem applicationInfo,
            Optional<ContainerImageBuildRequestBuildItem> buildRequest,
            Optional<ContainerImagePushRequestBuildItem> pushRequest,
            List<ContainerImageLabelBuildItem> containerImageLabels,
            BuildProducer<ArtifactResultBuildItem> artifactResultProducer,
            BuildProducer<ContainerImageResultBuildItem> containerImageResultProducer) {

        if (!containerImageConfig.build && !containerImageConfig.push && !buildRequest.isPresent()
                && !pushRequest.isPresent()) {
            return;
        }

        JibContainerBuilder jibContainerBuilder = createContainerBuilderFromJar(jibConfig,
                sourceJar, outputTarget, mainClass, containerImageLabels);
        JibContainer container = containerize(applicationInfo, containerImageConfig, jibContainerBuilder,
                pushRequest.isPresent());

        ImageReference targetImage = container.getTargetImage();
        containerImageResultProducer.produce(new ContainerImageResultBuildItem(JIB, container.getImageId().getHash(),
                targetImage.getRepository(), targetImage.getTag()));
        artifactResultProducer.produce(new ArtifactResultBuildItem(null, "jar-container", Collections.emptyMap()));
    }

    @BuildStep(onlyIf = { IsNormal.class, JibBuild.class, NativeBuild.class })
    public void buildFromNative(ContainerImageConfig containerImageConfig, JibConfig jibConfig,
            NativeImageBuildItem nativeImage,
            ApplicationInfoBuildItem applicationInfo,
            Optional<ContainerImageBuildRequestBuildItem> buildRequest,
            Optional<ContainerImagePushRequestBuildItem> pushRequest,
            List<ContainerImageLabelBuildItem> containerImageLabels,
            BuildProducer<ArtifactResultBuildItem> artifactResultProducer,
            BuildProducer<ContainerImageResultBuildItem> containerImageResultProducer) {

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
        JibContainer container = containerize(applicationInfo, containerImageConfig, jibContainerBuilder,
                pushRequest.isPresent());

        ImageReference targetImage = container.getTargetImage();
        containerImageResultProducer.produce(new ContainerImageResultBuildItem(JIB, container.getImageId().getHash(),
                targetImage.getRepository(), targetImage.getTag()));
        artifactResultProducer.produce(new ArtifactResultBuildItem(null, "native-container", Collections.emptyMap()));
    }

    private JibContainer containerize(ApplicationInfoBuildItem applicationInfo, ContainerImageConfig containerImageConfig,
            JibContainerBuilder jibContainerBuilder, boolean pushRequested) {
        Containerizer containerizer = createContainerizer(containerImageConfig, applicationInfo, pushRequested);
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
            ApplicationInfoBuildItem applicationInfo, boolean pushRequested) {
        Containerizer containerizer;
        ImageReference imageReference = getImageReference(containerImageConfig, applicationInfo);

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

    private ImageReference getImageReference(ContainerImageConfig containerImageConfig,
            ApplicationInfoBuildItem applicationInfo) {

        String registry = containerImageConfig.registry.orElse(null);
        if ((registry != null) && !ImageReference.isValidRegistry(registry)) {
            throw new IllegalArgumentException("The supplied container-image registry '" + registry + "' is invalid");
        }

        String repository = (containerImageConfig.group.map(s -> s + "/").orElse(""))
                + containerImageConfig.name.orElse(applicationInfo.getName());
        if (!ImageReference.isValidRepository(repository)) {
            throw new IllegalArgumentException("The supplied container-image repository '" + repository + "' is invalid");
        }

        String tag = containerImageConfig.tag.orElse(applicationInfo.getVersion());
        if (tag != null && !ImageReference.isValidTag(tag)) {
            throw new IllegalArgumentException("The supplied container-image tag '" + tag + "' is invalid");
        }

        return ImageReference.of(registry, repository, tag);
    }

    private JibContainerBuilder createContainerBuilderFromJar(JibConfig jibConfig,
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
                    .addClasses(classesDir, IS_CLASS_PREDICATE)
                    .addJvmFlags(jibConfig.jvmArguments)
                    .setMainClass(mainClassBuildItem.getClassName());
            if (sourceJarBuildItem.getLibraryDir() != null) {
                javaContainerBuilder
                        .addDependencies(
                                Files.list(sourceJarBuildItem.getLibraryDir())
                                        .filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".jar"))
                                        .sorted(Comparator.comparing(Path::getFileName))
                                        .collect(Collectors.toList()));
            }

            return javaContainerBuilder.toContainerBuilder()
                    .setEnvironment(jibConfig.environmentVariables)
                    .setLabels(allLabels(jibConfig, containerImageLabels))
                    .setCreationTime(Instant.now());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InvalidImageReferenceException e) {
            throw new RuntimeException(e);
        }
    }

    private JibContainerBuilder createContainerBuilderFromNative(ContainerImageConfig containerImageConfig, JibConfig jibConfig,
            NativeImageBuildItem nativeImageBuildItem, List<ContainerImageLabelBuildItem> containerImageLabels) {
        List<String> entrypoint = new ArrayList<>(jibConfig.nativeArguments.size() + 1);
        entrypoint.add("./" + BINARY_NAME_IN_CONTAINER);
        entrypoint.addAll(jibConfig.nativeArguments);
        try {
            AbsoluteUnixPath workDirInContainer = AbsoluteUnixPath.get("/work");
            return Jib
                    .from(toRegistryImage(ImageReference.parse(jibConfig.baseNativeImage), containerImageConfig.username,
                            containerImageConfig.password))
                    .addLayer(LayerConfiguration.builder()
                            .addEntry(nativeImageBuildItem.getPath(), workDirInContainer.resolve(BINARY_NAME_IN_CONTAINER),
                                    FilePermissions.fromOctalString("775"))
                            .build())
                    .setWorkingDirectory(workDirInContainer)
                    .setEntrypoint(entrypoint)
                    .setEnvironment(jibConfig.environmentVariables)
                    .setLabels(allLabels(jibConfig, containerImageLabels))
                    .setCreationTime(Instant.now());
        } catch (InvalidImageReferenceException e) {
            throw new RuntimeException(e);
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
