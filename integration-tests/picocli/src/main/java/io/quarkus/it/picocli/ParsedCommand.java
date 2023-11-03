package io.quarkus.it.picocli;

import jakarta.inject.Inject;

import picocli.CommandLine;

@CommandLine.Command
public class ParsedCommand implements Runnable {

    @CommandLine.Option(names = { "-p", "--parsed" }, description = "Will be parsed before run.")
    String name;

    @Inject
    ConfigFromParseResult configFromParseResult;

    @Override
    public void run() {
        System.out.println("Set value: " + name);
        System.out.println("Parsed value: " + configFromParseResult.getParsedName());
    }
}
