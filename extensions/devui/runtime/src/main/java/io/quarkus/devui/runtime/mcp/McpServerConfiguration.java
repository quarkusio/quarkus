package io.quarkus.devui.runtime.mcp;

import java.util.Properties;

public class McpServerConfiguration {

    private boolean enabled = false;

    public McpServerConfiguration() {

    }

    public McpServerConfiguration(Properties p) {
        if (p != null) {
            if (p.containsKey(ENABLED)) {
                this.enabled = Boolean.valueOf(p.getProperty(ENABLED, "false"));
            }
        }
        // TODO: Here we can add future configuration, like disable methods
    }

    public McpServerConfiguration(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnable(boolean enabled) {
        this.enabled = enabled;
    }

    public void enable() {
        this.setEnable(true);
    }

    public void disable() {
        this.setEnable(false);
    }

    public Properties toProperties() {
        Properties p = new Properties();
        p.setProperty(ENABLED, String.valueOf(this.enabled));
        return p;
    }

    private static final String ENABLED = "enabled";
}
