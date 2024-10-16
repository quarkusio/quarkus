package io.quarkus.gradle.tasks.worker;

import static io.quarkus.analytics.dto.segment.ContextBuilder.CommonSystemProperties.GRADLE_VERSION;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.gradle.api.GradleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.analytics.AnalyticsService;
import io.quarkus.analytics.config.FileLocationsImpl;
import io.quarkus.analytics.dto.segment.TrackEventType;
import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.app.ArtifactResult;
import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.AugmentResult;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.JarResult;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.maven.dependency.ResolvedDependency;

public abstract class BuildWorker extends QuarkusWorker<BuildWorkerParams> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BuildWorker.class);

    @Override
    public void execute() {
        BuildWorkerParams params = getParameters();
        Properties props = buildSystemProperties();

        ResolvedDependency appArtifact = params.getAppModel().get().getAppArtifact();
        String gav = appArtifact.getGroupId() + ":" + appArtifact.getArtifactId() + ":" + appArtifact.getVersion();
        LOGGER.info("Building Quarkus application {}", gav);
        LOGGER.info("  base name:                   {}", params.getBaseName().get());
        LOGGER.info("  target directory:            {}", params.getTargetDirectory().getAsFile().get());
        LOGGER.info("  configured JAR type:         {}", props.getProperty("quarkus.package.jar.type"));
        LOGGER.info("  configured output directory: {}", props.getProperty("quarkus.package.output-directory"));
        LOGGER.info("  configured output name:      {}", props.getProperty("quarkus.package.output-name"));
        LOGGER.info("  Gradle version:              {}", params.getGradleVersion().get());

        try (CuratedApplication appCreationContext = createAppCreationContext();
                AnalyticsService analyticsService = new AnalyticsService(
                        FileLocationsImpl.INSTANCE,
                        new Slf4JMessageWriter(LOGGER))) {

            // Processes launched from within the build task of Gradle (daemon) lose content
            // generated on STDOUT/STDERR by the process (see https://github.com/gradle/gradle/issues/13522).
            // We overcome this by letting build steps know that the STDOUT/STDERR should be explicitly
            // streamed, if they need to make available that generated data.
            // The io.quarkus.deployment.pkg.builditem.ProcessInheritIODisabled$Factory
            // does the necessary work to generate such a build item which the build step(s) can rely on
            AugmentAction augmentor = appCreationContext
                    .createAugmentor("io.quarkus.deployment.pkg.builditem.ProcessInheritIODisabled$Factory",
                            Collections.emptyMap());

            AugmentResult result = augmentor.createProductionApplication();
            if (result == null) {
                System.err.println("createProductionApplication() returned 'null' AugmentResult");
            } else {
                Map<String, Object> buildInfo = new HashMap<>(result.getGraalVMInfo());
                buildInfo.put(GRADLE_VERSION, params.getGradleVersion().get());
                analyticsService.sendAnalytics(
                        TrackEventType.BUILD,
                        appCreationContext.getApplicationModel(),
                        buildInfo,
                        params.getTargetDirectory().getAsFile().get());
                Path nativeResult = result.getNativeResult();
                LOGGER.info("AugmentResult.nativeResult = {}", nativeResult);
                List<ArtifactResult> results = result.getResults();
                if (results == null) {
                    LOGGER.warn("AugmentResult.results = null");
                } else {
                    LOGGER.info("AugmentResult.results = {}", results.stream().map(ArtifactResult::getPath)
                            .map(r -> r == null ? "null" : r.toString()).collect(Collectors.joining("\n    ", "\n    ", "")));
                }
                JarResult jar = result.getJar();
                LOGGER.info("AugmentResult:");
                if (jar == null) {
                    LOGGER.info("    .jar = null");
                } else {
                    LOGGER.info("    .jar.path = {}", jar.getPath());
                    LOGGER.info("    .jar.libraryDir = {}", jar.getLibraryDir());
                    LOGGER.info("    .jar.originalArtifact = {}", jar.getOriginalArtifact());
                    LOGGER.info("    .jar.uberJar = {}", jar.isUberJar());
                }
            }
            LOGGER.info("Quarkus application build was successful");
        } catch (BootstrapException e) {
            // Gradle "abbreviates" the stacktrace to something human-readable, but here the underlying cause might
            // get lost in the error output, so add 'e' to the message.
            throw new GradleException("Failed to build Quarkus application for " + gav + " due to " + e, e);
        }
    }

    private static class Slf4JMessageWriter implements MessageWriter {
        private final Logger logger;

        public Slf4JMessageWriter(final Logger logger) {
            this.logger = logger;
        }

        @Override
        public void info(String msg) {
            this.logger.info(msg);
        }

        @Override
        public void error(String msg) {
            this.logger.error(msg);
        }

        @Override
        public boolean isDebugEnabled() {
            return this.logger.isDebugEnabled();
        }

        @Override
        public void debug(String msg) {
            this.logger.debug(msg);
        }

        @Override
        public void warn(String msg) {
            this.logger.warn(msg);
        }
    }
}
