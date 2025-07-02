package io.quarkus.devui.runtime.mcp;

import java.time.Duration;

public interface McpServerRuntimeConfig {

    Duration connectionIdleTimeout();
}
