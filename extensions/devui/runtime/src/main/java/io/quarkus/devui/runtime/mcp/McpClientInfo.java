package io.quarkus.devui.runtime.mcp;

import java.util.Map;
import java.util.Objects;

public class McpClientInfo {

    private String name;
    private String version;

    public McpClientInfo() {
    }

    public McpClientInfo(String name, String version) {
        this.name = name;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 73 * hash + Objects.hashCode(this.name);
        hash = 73 * hash + Objects.hashCode(this.version);
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
        final McpClientInfo other = (McpClientInfo) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        return Objects.equals(this.version, other.version);
    }

    @Override
    public String toString() {
        return "McpClientInfo{" + "name=" + name + ", version=" + version + '}';
    }

    static McpClientInfo fromMap(Map map) {
        if (map != null) {
            McpClientInfo ci = new McpClientInfo();
            ci.setName((String) map.getOrDefault(NAME, UNKNOWN));
            ci.setVersion((String) map.getOrDefault(VERSION, ZERO));
            return ci;
        }
        return null;
    }

    private static final String NAME = "name";
    private static final String VERSION = "version";
    private static final String UNKNOWN = "Unknown MCP Client";
    private static final String ZERO = "0";
}
