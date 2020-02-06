package io.quarkus.jib.deployment;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.MainClassBuildItem;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.JarBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.pkg.steps.JarBuild;
import io.quarkus.deployment.pkg.steps.NativeBuild;

public class JibProcessor {

    private static final Logger log = Logger.getLogger(JibProcessor.class);

    private static final IsClassPredicate IS_CLASS_PREDICATE = new IsClassPredicate();

    @BuildStep(onlyIf = { IsNormal.class, JarBuild.class, JibEnabled.class }) // TODO: are we sure we only want it for normal execution?
    public void buildFromJar(
            // TODO: is this proper build item to consume?
            JarBuildItem sourceJarBuildItem,
            MainClassBuildItem mainClassBuildItem,
            OutputTargetBuildItem outputTargetBuildItem, ApplicationInfoBuildItem applicationInfo, JibConfig jibConfig,
            // TODO figure out the proper output
            BuildProducer<ArtifactResultBuildItem> producer) {
        if (!jibConfig.enabled) {
            log.debug("Jib container image build was disabled");
            return;
        }
        JibContainerBuilder jibContainerBuilder = createContainerBuilderFromJar(jibConfig, sourceJarBuildItem,
                outputTargetBuildItem,
                mainClassBuildItem);
        containerize(applicationInfo, jibConfig, jibContainerBuilder);
        producer.produce(new ArtifactResultBuildItem(null, "containerImageFromJar", Collections.emptyMap()));
    }

    @BuildStep(onlyIf = { IsNormal.class, NativeBuild.class, JibEnabled.class }) // TODO: are we sure we only want it for normal execution?
    public void buildFromNative(
            NativeImageBuildItem nativeImageBuildItem,
            ApplicationInfoBuildItem applicationInfo, JibConfig jibConfig,
            // TODO figure out the proper output
            BuildProducer<ArtifactResultBuildItem> producer) {
        if (!jibConfig.enabled) {
            log.debug("Jib container image build was disabled");
            return;
        }
        // TODO: we also need to make sure the native binary is a linux native binary
        JibContainerBuilder jibContainerBuilder = createContainerBuilderFromNative(jibConfig, nativeImageBuildItem);
        containerize(applicationInfo, jibConfig, jibContainerBuilder);
        producer.produce(new ArtifactResultBuildItem(null, "containerImageFromNative", Collections.emptyMap()));
    }

    private void containerize(ApplicationInfoBuildItem applicationInfo, JibConfig jibConfig,
            JibContainerBuilder jibContainerBuilder) {
        Containerizer containerizer = createContainerizer(jibConfig, applicationInfo);
        try {
            log.info("Starting container image build");
            JibContainer container = jibContainerBuilder.containerize(containerizer);
            log.infof("%s container image %s (%s)\n", jibConfig.push ? "Pushed" : "Created", container.getTargetImage(),
                    container.getDigest());
        } catch (Exception e) {
            log.error("Unable to create container image", e);
        }
    }

    private Containerizer createContainerizer(JibConfig jibConfig, ApplicationInfoBuildItem applicationInfo) {
        Containerizer containerizer;
        ImageReference imageReference = getImageReference(jibConfig, applicationInfo);
        if (jibConfig.push) {
            CredentialRetrieverFactory credentialRetrieverFactory = CredentialRetrieverFactory.forImage(imageReference,
                    log::info);
            RegistryImage registryImage = RegistryImage.named(imageReference);
            registryImage.addCredentialRetriever(credentialRetrieverFactory.wellKnownCredentialHelpers());
            registryImage.addCredentialRetriever(credentialRetrieverFactory.dockerConfig());
            if (jibConfig.username.isPresent() && jibConfig.password.isPresent()) {
                registryImage.addCredential(jibConfig.username.get(), jibConfig.password.get());
            }
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
        containerizer.setAllowInsecureRegistries(jibConfig.insecure);
        return containerizer;
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

    private ImageReference getImageReference(JibConfig jibConfig, ApplicationInfoBuildItem applicationInfo) {
        return ImageReference.of(jibConfig.registry, jibConfig.group + "/" + jibConfig.name.orElse(applicationInfo.getName()),
                jibConfig.tag.orElse(applicationInfo.getVersion()));
    }

    private JibContainerBuilder createContainerBuilderFromJar(JibConfig jibConfig, JarBuildItem sourceJarBuildItem,
            OutputTargetBuildItem outputTargetBuildItem, MainClassBuildItem mainClassBuildItem) {
        try {
            // not ideal since this has been previously zipped - we would like to just reuse it
            Path classesDir = outputTargetBuildItem.getOutputDirectory().resolve("jib");
            ZipUtils.unzip(sourceJarBuildItem.getPath(), classesDir);
            JavaContainerBuilder javaContainerBuilder = JavaContainerBuilder
                    .from(jibConfig.baseJvmImage)
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
                    .setEnvironment(jibConfig.environmentVariables.orElse(Collections.emptyMap()))
                    .setCreationTime(Instant.now());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InvalidImageReferenceException e) {
            throw new RuntimeException(e);
        }
    }

    private JibContainerBuilder createContainerBuilderFromNative(JibConfig jibConfig,
            NativeImageBuildItem nativeImageBuildItem) {
        final List<String> entrypoint = new ArrayList<>(jibConfig.nativeArguments.size() + 1);
        entrypoint.add("./application");
        entrypoint.addAll(jibConfig.nativeArguments);
        try {
            AbsoluteUnixPath workDirInContainer = AbsoluteUnixPath.get("/work");
            return Jib.from(jibConfig.baseNativeImage)
                    .addLayer(LayerConfiguration.builder()
                            .addEntry(nativeImageBuildItem.getPath(), workDirInContainer.resolve("application"),
                                    FilePermissions.fromOctalString("775"))
                            .build())
                    .setWorkingDirectory(workDirInContainer)
                    .setEntrypoint(entrypoint)
                    .setEnvironment(jibConfig.environmentVariables.orElse(Collections.emptyMap()))
                    .setCreationTime(Instant.now());
        } catch (InvalidImageReferenceException e) {
            throw new RuntimeException(e);
        }
    }

    // TODO: this predicate is rather simplistic since it results in creating the directory structure in both the resources and classes so it should probably be improved to remove empty directories
    private static class IsClassPredicate implements Predicate<Path> {

        @Override
        public boolean test(Path path) {
            return path.getFileName().toString().endsWith(".class");
        }
    }
}
