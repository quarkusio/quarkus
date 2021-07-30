package io.quarkus.deployment.console;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.aesh.readline.tty.terminal.TerminalConnection;
import org.aesh.terminal.Connection;

import io.quarkus.deployment.dev.testing.TestConfig;
import io.quarkus.dev.console.BasicConsole;
import io.quarkus.dev.console.QuarkusConsole;
import io.quarkus.dev.console.RedirectPrintStream;
import io.quarkus.runtime.console.ConsoleRuntimeConfig;
import io.quarkus.runtime.util.ColorSupport;

public class ConsoleHelper {

    public static synchronized void installConsole(TestConfig config, ConsoleConfig consoleConfig,
            ConsoleRuntimeConfig consoleRuntimeConfig, io.quarkus.runtime.logging.ConsoleConfig logConfig, boolean test) {
        if (QuarkusConsole.installed) {
            return;
        }
        boolean colorEnabled = ColorSupport.isColorEnabled(consoleRuntimeConfig, logConfig);
        QuarkusConsole.installed = true;
        //if there is no color we need a basic console
        //note that we never enable input for tests
        //surefire communicates of stdin, so this can mess with it
        boolean inputSupport = !test && !config.disableConsoleInput.orElse(consoleConfig.disableInput);
        if (!inputSupport) {
            //note that in this case we don't hold onto anything from this class loader
            //which is important for the test suite
            QuarkusConsole.INSTANCE = new BasicConsole(colorEnabled, false, System.out, System.console());
            return;
        }
        try {
            new TerminalConnection(new Consumer<Connection>() {
                @Override
                public void accept(Connection connection) {
                    if (connection.supportsAnsi() && !config.basicConsole.orElse(consoleConfig.basic)) {
                        QuarkusConsole.INSTANCE = new AeshConsole(connection);
                    } else {
                        LinkedBlockingDeque<Integer> queue = new LinkedBlockingDeque<>();
                        if (inputSupport) {
                            connection.openNonBlocking();
                        }
                        connection.setStdinHandler(new Consumer<int[]>() {
                            @Override
                            public void accept(int[] ints) {
                                for (int i : ints) {
                                    queue.add(i);
                                }
                            }
                        });
                        connection.setSignalHandler(event -> {
                            switch (event) {
                                case INT:
                                    //todo: why does async exit not work here
                                    //Quarkus.asyncExit();
                                    //end(conn);
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            System.exit(0);
                                        }
                                    }).start();
                                    break;
                            }
                        });
                        connection.setCloseHandler(new Consumer<Void>() {
                            @Override
                            public void accept(Void unused) {
                                queue.add(-1);
                            }
                        });
                        QuarkusConsole.INSTANCE = new BasicConsole(colorEnabled,
                                inputSupport,
                                connection::write, new Supplier<Integer>() {
                                    @Override
                                    public Integer get() {
                                        try {
                                            return queue.takeFirst();
                                        } catch (InterruptedException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                });
                    }
                }
            });
        } catch (IOException e) {
            QuarkusConsole.INSTANCE = new BasicConsole(colorEnabled, false, System.out, System.console());
        }

        RedirectPrintStream ps = new RedirectPrintStream();
        System.setOut(ps);
        System.setErr(ps);
    }
}
