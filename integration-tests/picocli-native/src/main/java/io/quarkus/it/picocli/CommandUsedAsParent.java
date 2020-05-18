package io.quarkus.it.picocli;

import picocli.CommandLine;

@CommandLine.Command(subcommands = ChildOfParentCommand.class)
public class CommandUsedAsParent {

    @CommandLine.Option(names = "-p", description = "Value read by child command.")
    String parentValue;

}
