package io.quarkus.deployment.builditem;

import org.aesh.command.Command;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.impl.container.AeshCommandContainer;
import org.aesh.command.impl.container.AeshCommandContainerBuilder;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.parser.CommandLineParserBuilder;
import org.aesh.command.parser.CommandLineParserException;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that registers a command to be available in the Quarkus console.
 * <p>
 * This allows developers to define custom commands that can be executed from the
 * Quarkus dev console or terminal. The command is wrapped in a {@link CommandContainer}
 * for use with the Aesh command-line framework.
 * </p>
 * <p>
 * The build item supports creating the command container from a raw {@link Command},
 * a {@link ProcessedCommand}, or a fully built {@link CommandContainer}.
 * </p>
 */
public final class ConsoleCommandBuildItem extends MultiBuildItem {

    final CommandContainer consoleCommand;

    /**
     * Creates a build item from an existing {@link CommandContainer}.
     *
     * @param consoleCommand the fully constructed command container
     */
    public ConsoleCommandBuildItem(CommandContainer consoleCommand) {
        this.consoleCommand = consoleCommand;
    }

    /**
     * Creates a build item from a {@link ProcessedCommand}, which is internally
     * wrapped in a {@link CommandContainer} using Aesh.
     *
     * @param consoleCommand the processed command definition
     */
    public ConsoleCommandBuildItem(ProcessedCommand consoleCommand) {
        this.consoleCommand = new AeshCommandContainer(
                CommandLineParserBuilder.builder()
                        .processedCommand(consoleCommand)
                        .create());
    }

    /**
     * Creates a build item from a user-defined {@link Command} implementation.
     * <p>
     * The command is wrapped using {@link AeshCommandContainerBuilder}.
     * </p>
     *
     * @param consoleCommand the custom command implementation
     * @throws RuntimeException if the command container cannot be created due to a parsing error
     */
    public ConsoleCommandBuildItem(Command<?> consoleCommand) {
        try {
            this.consoleCommand = new AeshCommandContainerBuilder().create(consoleCommand);
        } catch (CommandLineParserException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the command container that wraps the user-defined or processed console command.
     *
     * @return the {@link CommandContainer} instance
     */
    public CommandContainer getConsoleCommand() {
        return consoleCommand;
    }
}
