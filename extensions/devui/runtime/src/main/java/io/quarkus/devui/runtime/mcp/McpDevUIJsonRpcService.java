package io.quarkus.devui.runtime.mcp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.jboss.logging.Logger;

import io.quarkus.devui.runtime.jsonrpc.JsonRpcMethod;
import io.quarkus.devui.runtime.spi.McpEvent;
import io.quarkus.devui.runtime.spi.McpServerConfiguration;
import io.quarkus.runtime.annotations.Usage;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;

/**
 * Normal Dev UI Json RPC Service for the Dev MPC Screen
 */
@ApplicationScoped
public class McpDevUIJsonRpcService {
    private static final Logger LOG = Logger.getLogger(McpDevUIJsonRpcService.class.getName());

    private final BroadcastProcessor<McpClientInfo> connectedClientStream = BroadcastProcessor.create();
    private final BroadcastProcessor<McpEvent> mcpEventStream = BroadcastProcessor.create();

    // TODO: We need to be able to deregister a client if the connection drops. Maybe ping it ?

    private final Set<McpClientInfo> connectedClients = new HashSet<>();
    private McpServerConfiguration mcpServerConfiguration;

    @PostConstruct
    public void init() {
        this.mcpServerConfiguration = new McpServerConfiguration(loadConfiguration());
    }

    public Set<McpClientInfo> getConnectedClients() {
        if (!this.connectedClients.isEmpty()) {
            return this.connectedClients;
        }
        return null;
    }

    public Multi<McpClientInfo> getConnectedClientStream() {
        return this.connectedClientStream;
    }

    @Produces
    public Multi<McpEvent> getEventStream() {
        return this.mcpEventStream;
    }

    public void addClientInfo(McpClientInfo clientInfo) {
        this.connectedClients.add(clientInfo);
        this.connectedClientStream.onNext(clientInfo);
    }

    @Produces
    public McpServerConfiguration getMcpServerConfiguration() {
        return this.mcpServerConfiguration;
    }

    public McpServerConfiguration enable() {
        this.mcpServerConfiguration.enable();
        storeConfiguration(this.mcpServerConfiguration.toProperties());
        mcpEventStream.onNext(new McpEvent(true));
        return this.mcpServerConfiguration;
    }

    public McpServerConfiguration disable() {
        this.mcpServerConfiguration.disable();
        storeConfiguration(this.mcpServerConfiguration.toProperties());
        mcpEventStream.onNext(new McpEvent(false));
        return this.mcpServerConfiguration;
    }

    public McpServerConfiguration enableMethod(String name) {
        this.mcpServerConfiguration.enableMethod(name);
        storeConfiguration(this.mcpServerConfiguration.toProperties());
        return this.mcpServerConfiguration;
    }

    public McpServerConfiguration disableMethod(String name) {
        this.mcpServerConfiguration.disableMethod(name);
        storeConfiguration(this.mcpServerConfiguration.toProperties());
        return this.mcpServerConfiguration;
    }

    public boolean isExplicitlyEnabled(String methodName) {
        return this.mcpServerConfiguration.isExplicitlyEnabled(methodName);
    }

    public boolean isExplicitlyDisabled(String methodName) {
        return this.mcpServerConfiguration.isExplicitlyDisabled(methodName);
    }

    public boolean isEnabled(JsonRpcMethod method, Filter filter) {
        if (method.getUsage().contains(Usage.DEV_MCP)) {
            if (isExplicitlyEnabled(method.getMethodName())) {
                return filter.equals(Filter.enabled);
            } else if (isExplicitlyDisabled(method.getMethodName())) {
                return filter.equals(Filter.disabled);
            } else if (filter.equals(Filter.enabled)) {
                return method.isMcpEnabledByDefault();
            } else if (filter.equals(Filter.disabled)) {
                return !method.isMcpEnabledByDefault();
            }
        }
        return false;
    }

    private Properties loadConfiguration() {
        Properties existingProps = new Properties();

        // Load existing configuration if present
        if (Files.exists(configFile)) {
            try (InputStream in = Files.newInputStream(configFile)) {
                existingProps.load(in);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return existingProps;
    }

    private boolean storeConfiguration(Properties p) {
        try {
            Files.createDirectories(configDir);
            try (OutputStream out = Files.newOutputStream(configFile)) {
                p.store(out, "Dev MCP Configuration");
                return true;
            } catch (IOException ex) {
                LOG.error("Could not create config file for Dev MCP [" + configFile + "]", ex);
                return false;
            }
        } catch (IOException ex) {
            LOG.error("Could not create config directory for Dev MCP [" + configDir + "]", ex);
            return false;
        }
    }

    private final Path configDir = Paths.get(System.getProperty("user.home"), ".quarkus");
    private final Path configFile = configDir.resolve("dev-mcp.properties");
}
