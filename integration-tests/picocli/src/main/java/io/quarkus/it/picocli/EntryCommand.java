package io.quarkus.it.picocli;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;

@TopCommand
@CommandLine.Command(subcommands = GoodbyeCommand.class)
public class EntryCommand {
}
