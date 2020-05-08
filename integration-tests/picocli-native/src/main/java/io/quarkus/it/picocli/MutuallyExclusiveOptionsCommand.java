package io.quarkus.it.picocli;

import picocli.CommandLine;

@CommandLine.Command(name = "exclusivedemo")
public class MutuallyExclusiveOptionsCommand {

    @CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
    Exclusive exclusive;

    static class Exclusive {
        @CommandLine.Option(names = "-a", required = true)
        int a;
        @CommandLine.Option(names = "-b", required = true)
        int b;
        @CommandLine.Option(names = "-c", required = true)
        int c;
    }
}