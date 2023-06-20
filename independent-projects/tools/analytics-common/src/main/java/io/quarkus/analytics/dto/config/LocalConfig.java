package io.quarkus.analytics.dto.config;

import java.io.Serializable;

public class LocalConfig implements AnalyticsLocalConfig, Serializable {
    private boolean active;

    public LocalConfig(boolean active) {
        this.active = active;
    }

    public LocalConfig() {
    }

    @Override
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
