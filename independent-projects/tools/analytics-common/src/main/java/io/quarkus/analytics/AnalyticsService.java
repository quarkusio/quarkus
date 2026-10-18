package io.quarkus.analytics;

import static io.quarkus.analytics.config.ExtensionsFilter.onlyPublic;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_APP;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_BUILD;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_CI;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_CI_NAME;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_DETECTED;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_GRAALVM;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_GRADLE_VERSION;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_IP;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_JAVA;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_JAVA_VERSION;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_KUBERNETES;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_LOCALE_COUNTRY;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_LOCATION;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_MAVEN_VERSION;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_NAME;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_OS;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_OS_ARCH;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_QUARKUS;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_TIMEZONE;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_VENDOR;
import static io.quarkus.analytics.dto.segment.ContextBuilder.PROP_VERSION;
import static io.quarkus.analytics.dto.segment.ContextBuilder.VALUE_NULL_IP;
import static io.quarkus.analytics.dto.segment.ContextBuilder.CommonSystemProperties.GRAALVM_VERSION_DISTRIBUTION;
import static io.quarkus.analytics.dto.segment.ContextBuilder.CommonSystemProperties.GRAALVM_VERSION_JAVA;
import static io.quarkus.analytics.dto.segment.ContextBuilder.CommonSystemProperties.GRAALVM_VERSION_VERSION;
import static io.quarkus.analytics.dto.segment.ContextBuilder.CommonSystemProperties.GRADLE_VERSION;
import static io.quarkus.analytics.dto.segment.ContextBuilder.CommonSystemProperties.MAVEN_VERSION;
import static io.quarkus.analytics.rest.RestClient.DEFAULT_TIMEOUT;
import static io.quarkus.analytics.util.StringUtils.hashSHA256;
import static io.quarkus.maven.dependency.DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT;
import static java.lang.System.getProperty;
import static java.lang.System.getenv;
import static java.util.Optional.ofNullable;

import java.io.File;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.quarkus.analytics.config.FileLocations;
import io.quarkus.analytics.dto.config.Identity;
import io.quarkus.analytics.dto.segment.ContextBuilder;
import io.quarkus.analytics.dto.segment.Track;
import io.quarkus.analytics.dto.segment.TrackEventType;
import io.quarkus.analytics.dto.segment.TrackProperties;
import io.quarkus.analytics.rest.RestClient;
import io.quarkus.analytics.util.FileUtils;
import io.quarkus.analytics.util.PropertyUtils;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.maven.dependency.ArtifactCoords;

public class AnalyticsService implements AutoCloseable {
    private final Queue<CompletableFuture<HttpResponse<String>>> postFutures;

    final private RestClient restClient;
    final private ConfigService config;
    final private AnonymousUserId anonymousUserId;
    final private MessageWriter log;
    final FileLocations fileLocations;

    public AnalyticsService(final FileLocations fileLocations, MessageWriter log) {
        this.fileLocations = fileLocations;
        if (log == null) {
            this.log = MessageWriter.info();
            this.log.info("No logger provided, using default");
        } else {
            this.log = log;
        }
        this.postFutures = new ConcurrentLinkedQueue<>();
        this.restClient = new RestClient(this.log);
        this.anonymousUserId = AnonymousUserId.getInstance(fileLocations, this.log);
        this.config = new ConfigService(this.restClient, this.anonymousUserId, fileLocations, this.log);
    }

    public void buildAnalyticsUserInput(Function<String, String> analyticsEnabledSupplier) {
        this.config.userAcceptance(analyticsEnabledSupplier);
    }

    public void sendAnalytics(final TrackEventType trackEventType,
            ApplicationModel applicationModel,
            final Map<String, Object> buildInfo,
            final File localBuildDir) {

        if (this.config.isActive() &&
                this.config.isArtifactActive(
                        applicationModel.getAppArtifact().getGroupId(),
                        getQuarkusVersion(applicationModel))) {

            final Map<String, Object> context = createContextMap(applicationModel, buildInfo);
            sendIdentity(context);
            Track trackEvent = Track.builder()
                    .userId(anonymousUserId.getUuid())
                    .context(context)
                    .event(trackEventType)
                    .properties(TrackProperties.builder()
                            .appExtensions(createExtensionsPropertyValue(applicationModel))
                            .build())
                    .timestamp(Instant.now())
                    .build();
            postFutures.add(this.restClient.postTrack(trackEvent));
            try {
                FileUtils.overwrite(trackEvent,
                        Path.of(localBuildDir.getAbsolutePath(), fileLocations.lastTrackFileName()));
            } catch (IOException e) {
                log.warn("[Quarkus build analytics] Failed to write the last analytics file. " + e.getMessage());
            }
        }
    }

    @Override
    public void close() {
        try {
            // complete all. Normally, the queue should have only 1 element.
            CompletableFuture.allOf(postFutures.toArray(new CompletableFuture[0])).get(
                    PropertyUtils.getProperty("quarkus.analytics.timeout", DEFAULT_TIMEOUT),
                    TimeUnit.MILLISECONDS);
            if (log.isDebugEnabled() && !postFutures.isEmpty()) {
                log.debug("[Quarkus build analytics] Build analytics sent successfully. Sent event can be seen at .../target/" +
                        fileLocations.lastTrackFileName());
            }
        } catch (ExecutionException | TimeoutException e) {
            if (log.isDebugEnabled()) {
                log.debug("[Quarkus build analytics] Failed to send build analytics to Segment. " +
                        "Connection might not be available or is too slow: " +
                        e.getClass().getName() + ": " +
                        (e.getMessage() == null ? "(no message)" : e.getMessage()));
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("[Quarkus build analytics] Failed to send build analytics to Segment: " +
                        e.getClass().getName() + ": " +
                        (e.getMessage() == null ? "(no message)" : e.getMessage()));
            }
        }
    }

    List<TrackProperties.AppExtension> createExtensionsPropertyValue(ApplicationModel applicationModel) {
        return applicationModel.getDependencies().stream()
                .filter(dep -> dep.isResolved() &&
                        dep.isFlagSet(TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT) &&
                        onlyPublic(dep.getGroupId(), log))
                .map(dep -> TrackProperties.AppExtension.builder()
                        .groupId(dep.getGroupId())
                        .artifactId(dep.getArtifactId())
                        .version(dep.getVersion())
                        .build())
                .collect(Collectors.toList());
    }

    void sendIdentity(final Map<String, Object> context) {
        if (this.anonymousUserId.isNew()) { // fixme when inactive on 1st call it will not send identity.
            this.restClient.postIdentity(Identity.builder()
                    .userId(this.anonymousUserId.getUuid())
                    .context(context)
                    .timestamp(Instant.now())
                    .build());
        }
    }

    Map<String, Object> createContextMap(ApplicationModel applicationModel,
            Map<String, Object> buildInfo) {
        ArtifactCoords moduleId = applicationModel.getAppArtifact();

        return new ContextBuilder()
                .mapPair(PROP_APP)
                .pair(PROP_NAME, hashSHA256(moduleId.getGroupId() + ":" + moduleId.getArtifactId()))
                .pair(PROP_VERSION, hashSHA256(moduleId.getArtifactId() + ":" + moduleId.getVersion()))
                .build()
                .mapPair(PROP_JAVA)
                .pair(PROP_VENDOR, getProperty("java.vendor", "N/A"))
                .pair(PROP_VERSION, getProperty("java.version", "N/A"))
                .build()
                .mapPair(PROP_GRAALVM)
                .pair(PROP_VENDOR, ofNullable(buildInfo.get(GRAALVM_VERSION_DISTRIBUTION)).orElse("N/A"))
                .pair(PROP_VERSION, ofNullable(buildInfo.get(GRAALVM_VERSION_VERSION)).orElse("N/A"))
                .pair(PROP_JAVA_VERSION, ofNullable(buildInfo.get(GRAALVM_VERSION_JAVA)).orElse("N/A"))
                .build()
                .mapPair(PROP_BUILD)
                .pair(PROP_MAVEN_VERSION, ofNullable(buildInfo.get(MAVEN_VERSION)).orElse("N/A"))
                .pair(PROP_GRADLE_VERSION, ofNullable(buildInfo.get(GRADLE_VERSION)).orElse("N/A"))
                .build()
                .mapPair(PROP_QUARKUS)
                .pair(PROP_VERSION, getQuarkusVersion(applicationModel))
                .build()
                .pair(PROP_IP, VALUE_NULL_IP)
                .mapPair(PROP_LOCATION)
                .pair(PROP_LOCALE_COUNTRY, Locale.getDefault().getCountry())
                .build()
                .mapPair(PROP_OS)
                .pair(PROP_NAME, getProperty("os.name", "N/A"))
                .pair(PROP_VERSION, getProperty("os.version", "N/A"))
                .pair(PROP_OS_ARCH, getProperty("os.arch", "N/A"))
                .build()
                .mapPair(PROP_CI)
                .pair(PROP_CI_NAME, getBuildSystemName())
                .build()
                .mapPair(PROP_KUBERNETES)
                .pair(PROP_DETECTED, isKubernetesDetected())
                .build()
                .pair(PROP_TIMEZONE, ZoneId.systemDefault().getDisplayName(TextStyle.NARROW, Locale.ENGLISH))
                .build();
    }

    private String isKubernetesDetected() {
        return Boolean.toString(allEnvSet(
                "KUBERNETES_SERVICE_HOST",
                "KUBERNETES_SERVICE_PORT"));
    }

    private String getBuildSystemName() {
        String travis = getenv("TRAVIS");
        String user = getenv("USER");
        if ("true".equals(travis) && "travis".equals(user)) {
            return "travis";
        }

        if (allEnvSet("JENKINS_URL", "JENKINS_HOME", "WORKSPACE")) {
            return "jenkins";
        }

        if (allEnvSet("GITHUB_WORKFLOW", "GITHUB_WORKSPACE", "GITHUB_RUN_ID")) {
            return "github-actions";
        }

        // https://docs.microsoft.com/en-us/azure/devops/pipelines/build/variables?view=azure-devops&tabs=yaml
        if (allEnvSet("BUILD_REASON", "AGENT_JOBSTATUS")) {
            return "azure-pipelines";
        }

        return "unknown";
    }

    private boolean allEnvSet(String... names) {
        for (String name : names) {
            if (getenv(name) == null) {
                return false;
            }
        }
        return true;
    }

    private String getQuarkusVersion(ApplicationModel applicationModel) {
        final Collection<ArtifactCoords> platformBoms = applicationModel.getPlatforms().getImportedPlatformBoms();
        if (platformBoms.isEmpty()) {
            // Typically, this situation should result in a build error, but it's not up to this service to fail it
            return "N/A";
        }
        return platformBoms.stream()
                .filter(artifactCoords -> artifactCoords.getArtifactId().equals("quarkus-bom"))
                .map(ArtifactCoords::getVersion)
                .findFirst()
                .orElse("CUSTOM");
    }
}
