package io.quarkus.analytics.dto.config;

import java.io.Serializable;

public class LocalConfig implements AnalyticsLocalConfig, Serializable {
    private boolean disabled;

    public LocalConfig(boolean disabled) {
        this.disabled = disabled;
    }

    public LocalConfig() {
    }

    @Override
    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }
}
