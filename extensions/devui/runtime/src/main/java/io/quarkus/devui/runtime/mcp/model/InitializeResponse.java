package io.quarkus.devui.runtime.mcp.model;

/**
 * The Initialize response as per MCP Spec (2025-03-26)
 */
public class InitializeResponse {
    public String protocolVersion = "2025-03-26";
    public Capabilities capabilities = new Capabilities();
    public ServerInfo serverInfo;
    public String instructions = "This MCP Server expose internals of a Running (Dev Mode) Quarkus application";

    public InitializeResponse(String quarkusVersion) {
        this.serverInfo = new ServerInfo(quarkusVersion);
    }

    public class Capabilities {
        public Resources resources = new Resources();
        public Tools tools = new Tools();
    }

    public class Resources {
        public boolean subscribe = false;
        public boolean listChanged = false;
    }

    public class Tools {
        public boolean subscribe = false;
        public boolean listChanged = false;
    }

    public class ServerInfo {
        public String name = "Quarkus Dev MCP";
        public String version;

        ServerInfo(String version) {
            this.version = version;
        }
    }
}