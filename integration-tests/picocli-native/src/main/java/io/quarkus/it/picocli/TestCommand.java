package io.quarkus.it.picocli;

import java.net.InetAddress;
import java.net.Proxy;
import java.util.Map;

import picocli.CommandLine;

@CommandLine.Command(description = "Basic Tests for reflection.")
public class TestCommand extends BaseTestCommand {

    @CommandLine.Option(names = { "-p", "--private" }, description = "Test private field access.")
    private String privateEntry;

    @CommandLine.Option(names = { "-s", "--status" }, description = "Test enum with ${COMPLETION-CANDIDATES}")
    Status status;

    @CommandLine.Option(names = { "-h", "--proxyHost" })
    Map<Proxy.Type, InetAddress> proxies;

    @CommandLine.Parameters(description = "Test positional parameters")
    String[] positional;

    public String getPrivateEntry() {
        return privateEntry;
    }
}
