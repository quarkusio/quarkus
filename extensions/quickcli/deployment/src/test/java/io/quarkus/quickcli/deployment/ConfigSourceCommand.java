package io.quarkus.quickcli.deployment;

import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.quickcli.annotations.Command;
import io.quarkus.quickcli.annotations.Option;

@Command(name = "config-test", description = "Tests config source integration")
public class ConfigSourceCommand implements Runnable {

    @Option(names = { "-p", "--server.port" }, description = "Server port")
    String port;

    @Option(names = { "--app.name" }, description = "App name")
    String appName;

    @Override
    public void run() {
        Optional<String> portFromConfig = ConfigProvider.getConfig().getOptionalValue("server.port", String.class);
        Optional<String> nameFromConfig = ConfigProvider.getConfig().getOptionalValue("app.name", String.class);
        System.out.println("option.port=" + port);
        System.out.println("config.server.port=" + portFromConfig.orElse("MISSING"));
        System.out.println("option.appName=" + appName);
        System.out.println("config.app.name=" + nameFromConfig.orElse("MISSING"));
    }
}
