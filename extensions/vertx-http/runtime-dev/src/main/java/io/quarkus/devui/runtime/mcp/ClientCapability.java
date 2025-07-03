package io.quarkus.devui.runtime.mcp;

import java.util.Map;

/**
 * A capability supported by the client.
 */
record ClientCapability(String name, Map<String, Object> properties) {

    public static final String ROOTS = "roots";

    public static final String SAMPLING = "sampling";

}
