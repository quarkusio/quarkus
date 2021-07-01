package io.quarkus.deployment.dev.console;

import java.io.IOException;
import java.util.function.Consumer;

import org.aesh.readline.tty.terminal.TerminalConnection;
import org.aesh.terminal.Connection;

import io.quarkus.deployment.ConsoleConfig;
import io.quarkus.deployment.dev.testing.TestConfig;
import io.quarkus.dev.console.BasicConsole;
import io.quarkus.dev.console.QuarkusConsole;
import io.quarkus.dev.console.RedirectPrintStream;

public class ConsoleHelper {

    public static synchronized void installConsole(TestConfig config, ConsoleConfig consoleConfig) {
        if (QuarkusConsole.installed) {
            return;
        }
        QuarkusConsole.installed = true;
        if (config.basicConsole.orElse(consoleConfig.basic)) {
            QuarkusConsole.INSTANCE = new BasicConsole(config.disableColor.orElse(consoleConfig.disableColor),
                    !config.disableConsoleInput.orElse(consoleConfig.disableInput), System.out);
        } else {
            try {
                new TerminalConnection(new Consumer<Connection>() {
                    @Override
                    public void accept(Connection connection) {
                        if (connection.supportsAnsi()) {
                            QuarkusConsole.INSTANCE = new AeshConsole(connection,
                                    !config.disableConsoleInput.orElse(consoleConfig.disableInput));
                        } else {
                            connection.close();
                            QuarkusConsole.INSTANCE = new BasicConsole(config.disableColor.orElse(consoleConfig.disableColor),
                                    !config.disableConsoleInput.orElse(consoleConfig.disableInput),
                                    System.out);
                        }
                    }
                });
            } catch (IOException e) {
                QuarkusConsole.INSTANCE = new BasicConsole(config.disableColor.orElse(consoleConfig.disableColor),
                        !config.disableConsoleInput.orElse(consoleConfig.disableInput), System.out);
            }
        }
        RedirectPrintStream ps = new RedirectPrintStream();
        System.setOut(ps);
        System.setErr(ps);
    }
}
