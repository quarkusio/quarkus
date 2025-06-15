package io.quarkus.analytics.dto.config;

import java.io.Serializable;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Allow to configure build analytics behaviour by downloading a remote configuration file from a public location.
 */
public class RemoteConfig implements AnalyticsRemoteConfig, Serializable {

    private boolean active;
    private List<String> denyAnonymousIds;
    private List<String> denyQuarkusVersions;
    private Duration refreshInterval;

    public RemoteConfig() {
    }

    RemoteConfig(boolean active, List<String> denyUserIds, List<String> denyQuarkusVersions, Duration refreshInterval) {
        this.active = active;
        this.denyAnonymousIds = denyUserIds;
        this.denyQuarkusVersions = denyQuarkusVersions;
        this.refreshInterval = refreshInterval;
    }

    public static RemoteConfigBuilder builder() {
        return new RemoteConfigBuilder();
    }

    @Override
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    @JsonProperty("deny_anonymous_ids")
    public List<String> getDenyAnonymousIds() {
        return denyAnonymousIds;
    }

    public void setDenyAnonymousIds(List<String> denyAnonymousIds) {
        this.denyAnonymousIds = denyAnonymousIds;
    }

    @Override
    @JsonProperty("deny_quarkus_versions")
    public List<String> getDenyQuarkusVersions() {
        return denyQuarkusVersions;
    }

    public void setDenyQuarkusVersions(List<String> denyQuarkusVersions) {
        this.denyQuarkusVersions = denyQuarkusVersions;
    }

    @Override
    @JsonProperty("refresh_interval")
    public Duration getRefreshInterval() {
        return refreshInterval;
    }

    public void setRefreshInterval(Duration refreshInterval) {
        this.refreshInterval = refreshInterval;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        RemoteConfig that = (RemoteConfig) o;
        return active == that.active &&
                Objects.equals(denyAnonymousIds, that.denyAnonymousIds) &&
                Objects.equals(denyQuarkusVersions, that.denyQuarkusVersions) &&
                Objects.equals(refreshInterval, that.refreshInterval);
    }

    @Override
    public int hashCode() {
        return Objects.hash(active, denyAnonymousIds, denyQuarkusVersions, refreshInterval);
    }

    public static class RemoteConfigBuilder {
        private boolean active;
        private List<String> denyUserIds;
        private List<String> denyQuarkusVersions;
        private Duration refreshInterval;

        RemoteConfigBuilder() {
        }

        public RemoteConfigBuilder active(boolean active) {
            this.active = active;
            return this;
        }

        public RemoteConfigBuilder denyUserIds(List<String> denyUserIds) {
            this.denyUserIds = denyUserIds;
            return this;
        }

        public RemoteConfigBuilder denyQuarkusVersions(List<String> denyQuarkusVersions) {
            this.denyQuarkusVersions = denyQuarkusVersions;
            return this;
        }

        public RemoteConfigBuilder refreshInterval(Duration refreshInterval) {
            this.refreshInterval = refreshInterval;
            return this;
        }

        public RemoteConfig build() {
            return new RemoteConfig(active, denyUserIds, denyQuarkusVersions, refreshInterval);
        }

        public String toString() {
            return "RemoteConfig.RemoteConfigBuilder(active=" + this.active + ", denyUserIds=" + this.denyUserIds +
                    ", denyQuarkusVersions=" + this.denyQuarkusVersions + ", refreshInterval=" + this.refreshInterval + ")";
        }
    }
}
