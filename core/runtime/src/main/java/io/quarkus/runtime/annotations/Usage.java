package io.quarkus.runtime.annotations;

import java.util.EnumSet;

public enum Usage {
    DEV_UI,
    DEV_MCP;

    public static EnumSet<Usage> onlyDevUI() {
        return EnumSet.of(Usage.DEV_UI);
    }

    public static EnumSet<Usage> onlyDevMCP() {
        return EnumSet.of(Usage.DEV_MCP);
    }

    public static EnumSet<Usage> devUIandDevMCP() {
        return EnumSet.of(Usage.DEV_UI, Usage.DEV_MCP);
    }
}