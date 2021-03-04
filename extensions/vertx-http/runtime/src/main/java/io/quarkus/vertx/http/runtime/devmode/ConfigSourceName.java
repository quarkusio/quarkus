package io.quarkus.vertx.http.runtime.devmode;

import java.util.Objects;

public class ConfigSourceName implements Comparable<ConfigSourceName> {
    private static final String APPLICATION_PROPERTIES_CONFIG_SOURCE = "PropertiesConfigSource[source=application.properties]";
    private static final String SYSTEM_PROPERTIES_CONFIG_SOURCE = "SysPropConfigSource";
    private static final String ENVIRONMENT_CONFIG_SOURCE = "EnvConfigSource";
    private static final String PROPERTIES_CONFIG_SOURCE = "PropertiesConfigSource";
    private static final String DEFAULT_VALUES = "default values";
    private static final String SOURCE = "[source=";
    private static final int SOURCE_LENGTH = SOURCE.length();

    private int order;
    private String name;
    private String displayName;
    private int ordinal;
    private boolean editable;

    public ConfigSourceName() {
    }

    public ConfigSourceName(String name, int ordinal) {
        if (name == null)
            name = "Other";
        this.name = name;
        this.ordinal = ordinal;
        this.order = createOrder();
        this.displayName = createDisplayName();
        this.editable = createEditable();
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getOrdinal() {
        return ordinal;
    }

    public void setOrdinal(int ordinal) {
        this.ordinal = ordinal;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    private String createDisplayName() {
        if (name.contains(SOURCE)) {
            return name.substring(name.indexOf(SOURCE) + SOURCE_LENGTH, name.length() - 1);
        } else if (name.equals(DEFAULT_VALUES)) {
            return "Default values";
        } else if (name.equals(SYSTEM_PROPERTIES_CONFIG_SOURCE)) {
            return "System properties";
        } else if (name.equals(ENVIRONMENT_CONFIG_SOURCE)) {
            return "Environment variables";
        }
        return name;
    }

    private int createOrder() {
        if (name.equals(APPLICATION_PROPERTIES_CONFIG_SOURCE)) {
            return 1;
        } else if (name.startsWith(PROPERTIES_CONFIG_SOURCE)) {
            return 2;
        } else if (name.equals(DEFAULT_VALUES)) {
            return 3;
        } else if (name.equals(SYSTEM_PROPERTIES_CONFIG_SOURCE)) {
            return 9;
        } else if (name.equals(ENVIRONMENT_CONFIG_SOURCE)) {
            return 10;
        }

        return 5;
    }

    private boolean createEditable() {
        return this.ordinal <= 250;
    }

    @Override
    public int compareTo(ConfigSourceName o) {
        return Integer.compare(this.order, o.order);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + Objects.hashCode(this.name);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ConfigSourceName other = (ConfigSourceName) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
