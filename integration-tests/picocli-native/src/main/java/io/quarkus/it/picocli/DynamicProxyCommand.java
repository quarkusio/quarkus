package io.quarkus.it.picocli;

import java.time.LocalDateTime;

import picocli.CommandLine;

public interface DynamicProxyCommand {

    @CommandLine.Option(names = "-t", description = "Parsed time.")
    LocalDateTime time();

}
