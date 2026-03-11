package io.quarkus.cli;

import java.util.concurrent.Callable;

import io.quarkus.cli.common.HelpOption;
import io.quarkus.cli.common.OutputOptionMixin;
import io.quarkus.devtools.mcp.McpLifecycleServer;
import picocli.CommandLine;

@CommandLine.Command(name = "mcp-server", header = "Start the Quarkus MCP lifecycle server (stdio).", description = {
        "%nStarts a Model Context Protocol (MCP) server over stdin/stdout that can manage",
        "Quarkus application lifecycle (start, stop, restart, status, logs).",
        "%nThis server is designed to be used by AI coding agents (e.g. Claude Code)",
        "and survives application crashes, allowing agents to recover broken apps.",
        "%nConfigure in your MCP client:",
        "  {\"command\": \"quarkus\", \"args\": [\"mcp-server\"]}"
})
public class McpServer implements Callable<Integer> {

    @CommandLine.Mixin(name = "output")
    OutputOptionMixin output;

    @CommandLine.Mixin
    HelpOption helpOption;

    @Override
    public Integer call() throws Exception {
        McpLifecycleServer.main(new String[0]);
        return CommandLine.ExitCode.OK;
    }
}
