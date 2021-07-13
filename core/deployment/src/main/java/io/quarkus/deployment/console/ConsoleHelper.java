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
        Consumer<String> consumer = System.out::print;
        if (System.console() != null) {
            consumer = (s) -> {
                System.console().writer().print(s);
                System.console().writer().flush();
            };
        }
        //note that we never enable input for tests
        //surefire communicates of stdin, so this can mess with it
        boolean inputSupport = !test && !config.disableConsoleInput.orElse(consoleConfig.disableInput);
        try {
            new TerminalConnection(new Consumer<Connection>() {
                @Override
                public void accept(Connection connection) {
                    if (connection.supportsAnsi() && !config.basicConsole.orElse(consoleConfig.basic)) {
                        QuarkusConsole.INSTANCE = new AeshConsole(connection,
                                inputSupport);
                    } else {
                        LinkedBlockingDeque<Integer> queue = new LinkedBlockingDeque<>();
                        connection.openNonBlocking();
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
            QuarkusConsole.INSTANCE = new BasicConsole(colorEnabled,
                    inputSupport, consumer);
        }

        RedirectPrintStream ps = new RedirectPrintStream();
        System.setOut(ps);
        System.setErr(ps);
    }
}
