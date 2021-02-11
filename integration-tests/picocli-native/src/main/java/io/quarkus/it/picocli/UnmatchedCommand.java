package io.quarkus.it.picocli;

import java.util.List;

import picocli.CommandLine;

public class UnmatchedCommand {
    @CommandLine.Option(names = "-a")
    String alpha;
    @CommandLine.Option(names = "-b")
    String beta;
    @CommandLine.Parameters
    String[] remainder;
    @CommandLine.Unmatched
    List<String> unmatched;
}
