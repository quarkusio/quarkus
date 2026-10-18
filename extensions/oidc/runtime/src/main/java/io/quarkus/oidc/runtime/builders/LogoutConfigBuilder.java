package io.quarkus.oidc.runtime.builders;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import io.quarkus.oidc.OidcTenantConfigBuilder;
import io.quarkus.oidc.runtime.OidcTenantConfig;
import io.quarkus.oidc.runtime.OidcTenantConfig.Logout.ClearSiteData;
import io.quarkus.oidc.runtime.OidcTenantConfig.Logout.LogoutMode;

/**
 * Builder for the {@link OidcTenantConfig.Logout}.
 */
public final class LogoutConfigBuilder {
    private record LogoutImpl(Optional<String> path, Optional<String> postLogoutPath, String postLogoutUriParam,
            Map<String, String> extraParams, OidcTenantConfig.Backchannel backchannel,
            OidcTenantConfig.Frontchannel frontchannel,
            Optional<Set<ClearSiteData>> clearSiteData,
            LogoutMode logoutMode) implements OidcTenantConfig.Logout {
    }

    private record FrontchannelImpl(Optional<String> path) implements OidcTenantConfig.Frontchannel {
    }

    private final OidcTenantConfigBuilder builder;
    private final Map<String, String> extraParams = new HashMap<>();
    private Optional<String> path;
    private Optional<String> postLogoutPath;
    private String postLogoutUriParam;
    private OidcTenantConfig.Backchannel backchannel;
    private OidcTenantConfig.Frontchannel frontchannel;
    private Optional<Set<ClearSiteData>> clearSiteData = Optional.of(new HashSet<>());
    private LogoutMode logoutMode;

    public LogoutConfigBuilder() {
        this(new OidcTenantConfigBuilder());
    }

    public LogoutConfigBuilder(OidcTenantConfigBuilder builder) {
        this.builder = Objects.requireNonNull(builder);
        var logout = builder.getLogout();
        if (!logout.extraParams().isEmpty()) {
            this.extraParams.putAll(logout.extraParams());
        }
        this.path = logout.path();
        this.postLogoutPath = logout.postLogoutPath();
        this.postLogoutUriParam = logout.postLogoutUriParam();
        this.backchannel = logout.backchannel();
        this.frontchannel = logout.frontchannel();
        this.clearSiteData = logout.clearSiteData();
        this.logoutMode = logout.logoutMode();
    }

    /**
     * @param path {@link OidcTenantConfig.Frontchannel#path()}
     * @return this builder
     */
    public LogoutConfigBuilder frontchannelPath(String path) {
        this.frontchannel = new FrontchannelImpl(Optional.ofNullable(path));
        return this;
    }

    /**
     * @param path {@link OidcTenantConfig.Logout#path()}
     * @return this builder
     */
    public LogoutConfigBuilder path(String path) {
        this.path = Optional.ofNullable(path);
        return this;
    }

    /**
     * @param postLogoutPath {@link OidcTenantConfig.Logout#postLogoutPath()}
     * @return this builder
     */
    public LogoutConfigBuilder postLogoutPath(String postLogoutPath) {
        this.postLogoutPath = Optional.ofNullable(postLogoutPath);
        return this;
    }

    /**
     * @param postLogoutUriParam {@link OidcTenantConfig.Logout#postLogoutUriParam()}
     * @return this builder
     */
    public LogoutConfigBuilder postLogoutUriParam(String postLogoutUriParam) {
        this.postLogoutUriParam = Objects.requireNonNull(postLogoutUriParam);
        return this;
    }

    /**
     * @param extraParamKey {@link OidcTenantConfig.Logout#extraParams()} key
     * @param extraParamValue {@link OidcTenantConfig.Logout#extraParams()} value
     * @return this builder
     */
    public LogoutConfigBuilder extraParam(String extraParamKey, String extraParamValue) {
        Objects.requireNonNull(extraParamKey);
        Objects.requireNonNull(extraParamValue);
        this.extraParams.put(extraParamKey, extraParamValue);
        return this;
    }

    /**
     * @param extraParams {@link OidcTenantConfig.Logout#extraParams()}
     * @return this builder
     */
    public LogoutConfigBuilder extraParams(Map<String, String> extraParams) {
        if (extraParams != null) {
            this.extraParams.putAll(extraParams);
        }
        return this;
    }

    /**
     * Clear all site data
     *
     * @return this builder
     */
    public LogoutConfigBuilder clearSiteData() {
        this.clearSiteData(List.of(ClearSiteData.WILDCARD));
        return this;
    }

    /**
     * @param clear site data directives {@link OidcTenantConfig.Logout#clearSiteData()}
     * @return this builder
     */
    public LogoutConfigBuilder clearSiteData(List<ClearSiteData> directives) {
        Objects.requireNonNull(directives);
        this.clearSiteData.get().addAll(directives);
        return this;
    }

    public LogoutConfigBuilder logoutMode() {
        this.logoutMode(LogoutMode.QUERY);
        return this;
    }

    /**
     * @param clear site data directives {@link OidcTenantConfig.Logout#clearSiteData()}
     * @return this builder
     */
    public LogoutConfigBuilder logoutMode(LogoutMode logoutMode) {
        Objects.requireNonNull(logoutMode);
        this.logoutMode = logoutMode;
        return this;
    }

    /**
     * @param backchannel {@link OidcTenantConfig.Logout#backchannel()}
     * @return this builder
     */
    public LogoutConfigBuilder backchannel(OidcTenantConfig.Backchannel backchannel) {
        this.backchannel = Objects.requireNonNull(backchannel);
        return this;
    }

    /**
     * @return {@link OidcTenantConfig.Logout#backchannel()} builder
     */
    public BackchannelBuilder backchannel() {
        return new BackchannelBuilder(this);
    }

    /**
     * Builds {@link OidcTenantConfig.Logout} and returns {@link OidcTenantConfigBuilder}.
     *
     * @return OidcTenantConfigBuilder
     */
    public OidcTenantConfigBuilder end() {
        return builder.logout(build());
    }

    /**
     * @return built {@link OidcTenantConfig.Logout}
     */
    public OidcTenantConfig.Logout build() {
        return new LogoutImpl(path, postLogoutPath, postLogoutUriParam, Map.copyOf(extraParams), backchannel, frontchannel,
                clearSiteData, logoutMode);
    }

    /**
     * Builder for the {@link OidcTenantConfig.Backchannel}.
     */
    public static final class BackchannelBuilder {

        private record BackchannelImpl(Optional<String> path, int tokenCacheSize, Duration tokenCacheTimeToLive,
                Optional<Duration> cleanUpTimerInterval, String logoutTokenKey) implements OidcTenantConfig.Backchannel {
        }

        private final LogoutConfigBuilder logoutBuilder;
        private Optional<String> path;
        private int tokenCacheSize;
        private Duration tokenCacheTimeToLive;
        private Optional<Duration> cleanUpTimerInterval;
        private String logoutTokenKey;

        public BackchannelBuilder() {
            this(new LogoutConfigBuilder());
        }

        public BackchannelBuilder(LogoutConfigBuilder logoutBuilder) {
            this.logoutBuilder = Objects.requireNonNull(logoutBuilder);
            var backchannel = logoutBuilder.backchannel;
            this.path = backchannel.path();
            this.tokenCacheSize = backchannel.tokenCacheSize();
            this.tokenCacheTimeToLive = backchannel.tokenCacheTimeToLive();
            this.cleanUpTimerInterval = backchannel.cleanUpTimerInterval();
            this.logoutTokenKey = backchannel.logoutTokenKey();
        }

        /**
         * @param cleanUpTimerInterval {@link OidcTenantConfig.Backchannel#cleanUpTimerInterval()}
         * @return this builder
         */
        public BackchannelBuilder cleanUpTimerInterval(Duration cleanUpTimerInterval) {
            this.cleanUpTimerInterval = Optional.ofNullable(cleanUpTimerInterval);
            return this;
        }

        /**
         * @param logoutTokenKey {@link OidcTenantConfig.Backchannel#logoutTokenKey()}
         * @return this builder
         */
        public BackchannelBuilder logoutTokenKey(String logoutTokenKey) {
            this.logoutTokenKey = Objects.requireNonNull(logoutTokenKey);
            return this;
        }

        /**
         * @param tokenCacheTimeToLive {@link OidcTenantConfig.Backchannel#tokenCacheTimeToLive()}
         * @return this builder
         */
        public BackchannelBuilder tokenCacheTimeToLive(Duration tokenCacheTimeToLive) {
            this.tokenCacheTimeToLive = Objects.requireNonNull(tokenCacheTimeToLive);
            return this;
        }

        /**
         * @param tokenCacheSize {@link OidcTenantConfig.Backchannel#tokenCacheSize()}
         * @return this builder
         */
        public BackchannelBuilder tokenCacheSize(int tokenCacheSize) {
            this.tokenCacheSize = tokenCacheSize;
            return this;
        }

        /**
         * @param path {@link OidcTenantConfig.Backchannel#path()}
         * @return this builder
         */
        public BackchannelBuilder path(String path) {
            this.path = Optional.ofNullable(path);
            return this;
        }

        /**
         * Builds {@link OidcTenantConfig.Logout} with this {@link OidcTenantConfig.Backchannel} and returns the
         * {@link OidcTenantConfigBuilder} builder.
         *
         * @return OidcTenantConfigBuilder
         */
        public OidcTenantConfigBuilder endLogout() {
            return end().end();
        }

        /**
         * Builds {@link OidcTenantConfig.Backchannel} and returns the {@link LogoutConfigBuilder} builder.
         *
         * @return LogoutBuilder
         */
        public LogoutConfigBuilder end() {
            return logoutBuilder.backchannel(build());
        }

        /**
         * @return built {@link OidcTenantConfig.Backchannel}
         */
        public OidcTenantConfig.Backchannel build() {
            return new BackchannelImpl(path, tokenCacheSize, tokenCacheTimeToLive, cleanUpTimerInterval, logoutTokenKey);
        }
    }
}
