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
 * A {@link MultiBuildItem} used to register console commands.
 * <p>
 * Extensions can produce this build item to contribute AEsh commands that are
 * made available in the Quarkus interactive console.
 */
public final class ConsoleCommandBuildItem extends MultiBuildItem {

    /**
     * The command container registered by this build item. This is the actual command that will be
     * executed when the user invokes the command in the console.
     */
    final CommandContainer consoleCommand;

    /**
     * Creates a build item from an already built {@link CommandContainer}.
     *
     * @param consoleCommand the command container to register
     */
    public ConsoleCommandBuildItem(CommandContainer consoleCommand) {
        this.consoleCommand = consoleCommand;
    }

    /**
     * Creates a build item from an AEsh {@link ProcessedCommand}.
     *
     * @param consoleCommand the processed command metadata
     */
    public ConsoleCommandBuildItem(ProcessedCommand consoleCommand) {
        this.consoleCommand = new AeshCommandContainer(
                CommandLineParserBuilder.builder()
                        .processedCommand(consoleCommand)
                        .create());
    }

    /**
     * Creates a build item from a command implementation.
     *
     * @param consoleCommand the command implementation to register
     */
    public ConsoleCommandBuildItem(Command<?> consoleCommand) {
        try {
            this.consoleCommand = new AeshCommandContainerBuilder().create(consoleCommand);
        } catch (CommandLineParserException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return the command container registered by this build item
     */
    public CommandContainer getConsoleCommand() {
        return consoleCommand;
    }
}
