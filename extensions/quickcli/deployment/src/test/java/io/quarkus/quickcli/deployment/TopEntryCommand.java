package io.quarkus.quickcli.deployment;

import io.quarkus.quickcli.annotations.Command;
import io.quarkus.quickcli.runtime.annotations.TopCommand;

@TopCommand
@Command(name = "app", description = { "Top-level command" }, subcommands = { GoodbyeCommand.class })
public class TopEntryCommand {
}
