package io.quarkus.devui.runtime.spi;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class McpServerConfiguration {

    private boolean enabled = false;

    private Map userEnabledment = new HashMap();

    public McpServerConfiguration() {

    }

    public McpServerConfiguration(Properties p) {
        if (p != null) {
            if (p.containsKey(ENABLED)) {
                this.enabled = Boolean.parseBoolean(p.getProperty(ENABLED, "false"));
                p.remove(ENABLED);
            }
            userEnabledment = p;
        }
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

    public void enableMethod(String name) {
        if (isExplicitlyDisabled(name)) {
            this.userEnabledment.remove(name); // Revert to default
        } else {
            this.userEnabledment.put(name, "true");
        }
    }

    public void disableMethod(String name) {
        if (isExplicitlyEnabled(name)) {
            this.userEnabledment.remove(name); // Revert to default
        } else {
            this.userEnabledment.put(name, "false");
        }
    }

    public boolean isExplicitlyDisabled(String name) {
        return (this.userEnabledment.containsKey(name) && this.userEnabledment.get(name).equals("false"));
    }

    public boolean isExplicitlyEnabled(String name) {
        return (this.userEnabledment.containsKey(name) && this.userEnabledment.get(name).equals("true"));
    }

    public Properties toProperties() {
        Properties p = new Properties();
        p.setProperty(ENABLED, String.valueOf(this.enabled));
        p.putAll(userEnabledment);
        return p;
    }

    private static final String ENABLED = "enabled";

}
