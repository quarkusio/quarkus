package io.quarkus.it.picocli;

import picocli.CommandLine;

class NameMixin {
    @CommandLine.Option(names = { "-n", "--name" }, description = "Some name")
    String name;
}
