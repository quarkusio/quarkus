package io.quarkus.analytics;

import static io.quarkus.analytics.util.PropertyUtils.getProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import io.quarkus.analytics.config.FileLocations;
import io.quarkus.analytics.config.GroupIdFilter;
import io.quarkus.analytics.dto.config.AnalyticsRemoteConfig;
import io.quarkus.analytics.dto.config.LocalConfig;
import io.quarkus.analytics.dto.config.NoopRemoteConfig;
import io.quarkus.analytics.dto.config.RemoteConfig;
import io.quarkus.analytics.rest.ConfigClient;
import io.quarkus.analytics.util.FileUtils;
import io.quarkus.devtools.messagewriter.MessageWriter;

/**
 * Decided the build analytics behaviour. Retrieves, stores and provides the configuration.
 */
public class ConfigService {
    public static final String QUARKUS_ANALYTICS_DISABLED_LOCAL_PROP = "quarkus.analytics.disabled";
    public static final String QUARKUS_ANALYTICS_PROMPT_TIMEOUT = "quarkus.analytics.prompt.timeout";
    private static final String NEW_LINE = System.lineSeparator();
    public static final String ACCEPTANCE_PROMPT = NEW_LINE
            + "----------------------------" + NEW_LINE
            + "--- Help improve Quarkus ---" + NEW_LINE
            + "----------------------------" + NEW_LINE
            + "* Learn more: https://quarkus.io/usage/" + NEW_LINE
            + "* Do you agree to contribute anonymous build time data to the Quarkus community? (y/n and then enter) " + NEW_LINE;
    private static final int DEFAULT_REFRESH_HOURS = 12;

    private AnalyticsRemoteConfig config;
    private Instant lastRefreshTime;
    final private ConfigClient client;
    final private AnonymousUserId userId;
    final private Path remoteConfigFile;
    final private Path localConfigFile;
    final private MessageWriter log;

    private static Instant initLastRefreshTime(final Path configFile) {
        if (Files.exists(configFile)) {
            try {
                return Files.getLastModifiedTime(configFile).toInstant();
            } catch (IOException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    public ConfigService(final ConfigClient client, final AnonymousUserId userId, final FileLocations fileLocations,
            final MessageWriter log) {
        this.client = client;
        this.userId = userId;
        this.log = log;
        this.lastRefreshTime = initLastRefreshTime(fileLocations.getRemoteConfigFile());
        this.remoteConfigFile = fileLocations.getRemoteConfigFile();
        this.localConfigFile = fileLocations.getLocalConfigFile();
        loadConfig(RemoteConfig.class, remoteConfigFile)
                .ifPresentOrElse(c -> this.config = c, this::loadConfigFromInternet);
    }

    public void userAcceptance(Function<String, String> analyticsEnabledSupplier) {
        final int timeout = getProperty(QUARKUS_ANALYTICS_PROMPT_TIMEOUT, 10);
        if (Files.exists(localConfigFile) || getProperty(QUARKUS_ANALYTICS_DISABLED_LOCAL_PROP, false)) {
            return; // ask nothing
        } else {
            try {
                CompletableFuture<String> userInputFuture = CompletableFuture
                        .supplyAsync(() -> analyticsEnabledSupplier.apply(ACCEPTANCE_PROMPT));
                final String userInput = userInputFuture.get(timeout, TimeUnit.SECONDS).toLowerCase().trim();
                if (!validInput(userInput)) {
                    log.info("[Quarkus build analytics] Didn't receive a valid user's answer: `y` or `n`. " +
                            "The question will be asked again next time." + NEW_LINE);
                    return;
                }
                final boolean isActive = userInput.equals("y") || userInput.equals("yes") || userInput.startsWith("yy");
                FileUtils.createFileAndParent(localConfigFile);
                final boolean isDisabled = !isActive;// just to make it explicit
                FileUtils.write(new LocalConfig(isDisabled), localConfigFile);
                log.info("[Quarkus build analytics] Quarkus Build Analytics " + (isActive ? "enabled" : "disabled")
                        + " by the user." + NEW_LINE);
            } catch (TimeoutException e) {
                log.info("[Quarkus build analytics] Didn't receive the user's answer after " + timeout + " seconds. " +
                        "The question will be asked again next time." + NEW_LINE);
            } catch (Exception e) {
                log.info("[Quarkus build analytics] Analytics config file was not written successfully. " +
                        e.getClass().getName() + ": " + (e.getMessage() == null ? "(no message)" : e.getMessage()));
            }
        }
    }

    /**
     * True if build time analytics can be gathered.
     * <p>
     * <p>
     * Disabled by default.
     * <p>
     * If Not explicitly approved by user in dev mode, false
     * <p>
     * If analytics disabled by local property, false
     * <p>
     * If remote config not accessible, false
     * <p>
     * If disabled by remote config, false
     *
     * @return true if active
     */
    public boolean isActive() {
        if (!Files.exists(localConfigFile)) {
            return false; // disabled because user has not decided yet
        } else if (!loadConfig(LocalConfig.class, localConfigFile)
                .map(localConfig -> !localConfig.isDisabled())
                .orElse(true)) {
            return false; // disabled by the user and recorded on the local config
        }

        if (getProperty(QUARKUS_ANALYTICS_DISABLED_LOCAL_PROP, false)) {
            return false; // disabled by local property
        }
        AnalyticsRemoteConfig analyticsRemoteConfig = getRemoteConfig();
        return analyticsRemoteConfig.isActive() && isUserEnabled(analyticsRemoteConfig, userId.getUuid());
    }

    /**
     * If groupId has been disabled by local static config, false
     * If Quarkus version has been disabled by remote config, false
     *
     * @param groupId
     * @param quarkusVersion
     * @return true if active
     */
    public boolean isArtifactActive(final String groupId, final String quarkusVersion) {
        return GroupIdFilter.isAuthorizedGroupId(groupId, log) &&
                this.getRemoteConfig().getDenyQuarkusVersions().stream()
                        .noneMatch(version -> version.equals(quarkusVersion));
    }

    boolean isUserEnabled(final AnalyticsRemoteConfig analyticsRemoteConfig, final String user) {
        return analyticsRemoteConfig.getDenyAnonymousIds().stream()
                .noneMatch(uId -> uId.equals(user));
    }

    private boolean validInput(String input) {
        String[] allowedValues = { "n", "nn", "no", "y", "yy", "yes" };
        for (String allowedValue : allowedValues) {
            if (input.equalsIgnoreCase(allowedValue)) {
                return true;
            }
        }
        return false;
    }

    private AnalyticsRemoteConfig getRemoteConfig() {
        if (shouldRefresh()) {
            loadConfigFromInternet();
        }
        return this.config;
    }

    private boolean shouldRefresh() {
        return lastRefreshTime == null || Duration.between(
                lastRefreshTime,
                Instant.now()).compareTo(
                        this.config.getRefreshInterval()) > 0;
    }

    private <T> Optional<T> loadConfig(Class<T> clazz, Path file) {
        try {
            if (Files.exists(file)) {
                return FileUtils.read(clazz, file, log);
            }
            return Optional.empty();
        } catch (IOException e) {
            log.warn("[Quarkus build analytics] Failed to read " + file.getFileName() + ". Exception: " + e.getMessage());
            return Optional.empty();
        }
    }

    private void loadConfigFromInternet() {
        AnalyticsRemoteConfig analyticsRemoteConfig = this.client.getConfig().orElse(checkAgainConfig());
        this.lastRefreshTime = Instant.now();
        this.config = storeRemoteConfigOnDisk(analyticsRemoteConfig);
    }

    private AnalyticsRemoteConfig storeRemoteConfigOnDisk(AnalyticsRemoteConfig config) {
        try {
            if (!Files.exists(remoteConfigFile)) {
                FileUtils.createFileAndParent(remoteConfigFile);
            }
            FileUtils.write(config, remoteConfigFile);
            return config;
        } catch (IOException e) {
            log.warn("[Quarkus build analytics] Failed to save remote config file. Analytics will be skipped. Exception: "
                    + e.getMessage());
            return NoopRemoteConfig.INSTANCE;// disable
        }
    }

    private AnalyticsRemoteConfig checkAgainConfig() {
        return RemoteConfig.builder()
                .active(false)
                .denyQuarkusVersions(Collections.emptyList())
                .denyUserIds(Collections.emptyList())
                .refreshInterval(Duration.ofHours(DEFAULT_REFRESH_HOURS)).build();
    }
}
