package io.quarkus.it.picocli;

import picocli.CommandLine;

@CommandLine.Command(resourceBundle = "i18s.LocalizedCommandTwo")
public class LocalizedCommandTwo {
    @CommandLine.Option(names = "--first")
    String firstOption;
}
