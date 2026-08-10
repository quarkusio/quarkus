package io.quarkus.devshell.deployment;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.devshell.deployment.tui.RawTerminalBackend;
import io.quarkus.devshell.deployment.tui.TerminalUI;
import io.quarkus.devshell.deployment.tui.screens.MainMenuScreen;

/**
 * Standalone launcher for Dev Shell.
 * Connects to a running Quarkus dev mode instance from a separate terminal.
 * <p>
 * Usage: java -cp ... io.quarkus.devshell.deployment.DevShellCli [options] [port]
 */
public class DevShellCli {

    public static void main(String[] args) {
        String host = "localhost";
        int port = 8080;
        String basePath = "/q/dev-ui/json-rpc-ws";
        List<String> allowedHosts = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--host":
                    if (i + 1 < args.length) {
                        host = args[++i];
                    }
                    break;
                case "--port":
                case "-p":
                    if (i + 1 < args.length) {
                        port = Integer.parseInt(args[++i]);
                    }
                    break;
                case "--allow-host":
                    if (i + 1 < args.length) {
                        allowedHosts.add(args[++i]);
                    }
                    break;
                case "--help":
                case "-h":
                    printUsage();
                    return;
                default:
                    try {
                        port = Integer.parseInt(args[i]);
                    } catch (NumberFormatException e) {
                        System.err.println("Unknown argument: " + args[i]);
                        printUsage();
                        System.exit(1);
                    }
                    break;
            }
        }

        System.out.println("Connecting to Quarkus Dev Mode at " + host + ":" + port + "...");

        RawTerminalBackend backend = new RawTerminalBackend();
        DevShellJsonRpcClient jsonRpcClient = new DevShellJsonRpcClient(host, port, basePath, allowedHosts);

        try {
            jsonRpcClient.connect();
            System.out.println("Connected. Launching Dev Shell...");

            backend.start();
            backend.enterAlternateScreen();

            TerminalUI tui = new TerminalUI(backend, jsonRpcClient);
            tui.start(new MainMenuScreen());
        } catch (SecurityException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Failed to connect: " + e.getMessage());
            System.exit(1);
        } finally {
            backend.close();
            jsonRpcClient.close();
        }
    }

    private static void printUsage() {
        System.out.println("Quarkus Dev Shell - Terminal UI for Dev Mode");
        System.out.println();
        System.out.println("Usage: dev-shell [options] [port]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --host <host>         Host to connect to (default: localhost)");
        System.out.println("  --port, -p <port>     Port to connect to (default: 8080)");
        System.out.println("  --allow-host <host>   Allow connecting to a non-localhost host");
        System.out.println("  --help, -h            Show this help");
    }
}
