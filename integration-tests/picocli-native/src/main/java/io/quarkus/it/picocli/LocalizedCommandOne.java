package io.quarkus.it.picocli;

import picocli.CommandLine;

@CommandLine.Command(resourceBundle = "i18s.LocalizedCommandOne")
public class LocalizedCommandOne {
    @CommandLine.Option(names = "--first")
    String firstOption;
}
