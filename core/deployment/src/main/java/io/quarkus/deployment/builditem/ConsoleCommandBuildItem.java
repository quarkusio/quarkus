package io.quarkus.deployment.builditem;

import org.aesh.command.Command;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.impl.container.AeshCommandContainer;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.parser.CommandLineParserBuilder;
import org.aesh.command.parser.CommandLineParserException;

import io.quarkus.builder.item.MultiBuildItem;

public final class ConsoleCommandBuildItem extends MultiBuildItem {

    final CommandContainer consoleCommand;

    public ConsoleCommandBuildItem(CommandContainer consoleCommand) {
        this.consoleCommand = consoleCommand;
    }

    public ConsoleCommandBuildItem(ProcessedCommand consoleCommand) {
        this.consoleCommand = new AeshCommandContainer(
                CommandLineParserBuilder.builder()
                        .processedCommand(consoleCommand)
                        .create());
    }

    public ConsoleCommandBuildItem(Command<?> consoleCommand) {
        try {
            this.consoleCommand = new AeshCommandContainerBuilder().create(consoleCommand);
        } catch (CommandLineParserException e) {
            throw new RuntimeException(e);
        }
    }

    public CommandContainer getConsoleCommand() {
        return consoleCommand;
    }
}
