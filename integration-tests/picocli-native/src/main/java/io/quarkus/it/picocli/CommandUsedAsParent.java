package io.quarkus.it.picocli;

import picocli.CommandLine;

@CommandLine.Command(subcommands = ChildOfParentCommand.class, name = "command-used-as-parent")
public class CommandUsedAsParent {

    @CommandLine.Option(names = "-p", description = "Value read by child command.")
    String parentValue;

}
