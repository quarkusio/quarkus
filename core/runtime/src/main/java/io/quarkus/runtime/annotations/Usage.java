package io.quarkus.runtime.annotations;

import java.util.EnumSet;

public enum Usage {
    DEV_UI,
    MCP;

    public static EnumSet<Usage> onlyDevUI() {
        return EnumSet.of(Usage.DEV_UI);
    }

    public static EnumSet<Usage> devUIandMCP() {
        return EnumSet.of(Usage.DEV_UI, Usage.MCP);
    }
}