package io.quarkus.it.picocli;

import picocli.CommandLine;

@CommandLine.Command(name = "localized-command-two", mixinStandardHelpOptions = true, resourceBundle = "i18s.LocalizedCommandTwo")
public class LocalizedCommandTwo {
    @CommandLine.Option(names = "--first")
    String firstOption;
}
