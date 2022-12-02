package io.quarkus.it.picocli;

import java.net.InetAddress;
import java.net.Proxy;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Callable;

import picocli.CommandLine;

@CommandLine.Command(name = "test-command", description = "Basic Tests for reflection.")
public class TestCommand extends BaseTestCommand implements Callable<Integer> {

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

    @Override
    public Integer call() throws Exception {
        System.out.println("-p:" + privateEntry);
        System.out.println("-s:" + status);
        System.out.println("-h:" + proxies);
        System.out.println("positional:" + Arrays.toString(positional));
        return 0;
    }
}
