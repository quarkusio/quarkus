package io.quarkus.deployment.console;

import java.util.Collections;
import java.util.List;

import org.aesh.command.container.CommandContainer;

public class ConsoleCliManager {

    static List<CommandContainer> commands = Collections.emptyList();

    public static void setCommands(List<CommandContainer> commandList) {
        commands = commandList;
    }

}
