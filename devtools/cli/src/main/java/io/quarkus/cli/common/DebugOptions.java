package io.quarkus.cli.common;

import java.util.Collection;

import picocli.CommandLine;

public class DebugOptions {
    final static String LOCALHOST = "localhost";
    final static int DEFAULT_PORT = 5005;

    public enum DebugMode {
        connect,
        listen
    }

    @CommandLine.Option(order = 7, names = {
            "--no-debug" }, description = "Toggle debug mode. Enabled by default.", negatable = true)
    public boolean debug = true;

    @CommandLine.Option(order = 8, names = {
            "--debug-host" }, description = "Debug host, e.g. localhost or 0.0.0.0", defaultValue = LOCALHOST)
    public String host = LOCALHOST;

    @CommandLine.Option(order = 9, names = {
            "--debug-mode" }, description = "Valid values: ${COMPLETION-CANDIDATES}.%nEither connect to or listen on <host>:<port>.", defaultValue = "listen")
    public DebugMode mode = DebugMode.listen;

    @CommandLine.Option(order = 10, names = {
            "--debug-port" }, description = "Debug port (must be a number > 0).", defaultValue = "" + DEFAULT_PORT)
    public int port = DEFAULT_PORT;

    @CommandLine.Option(order = 11, names = {
            "--suspend" }, description = "In listen mode, suspend until a debugger is attached. Disabled by default.", negatable = true)
    public boolean suspend = false;

    public String getJvmDebugParameter() {
        return "-agentlib:jdwp=transport=dt_socket"
                + ",address=" + host + ":" + port
                + ",server=" + (mode == DebugMode.listen ? "y" : "n")
                + ",suspend=" + (suspend ? "y" : "n");
    }

    public void addDebugArguments(Collection<String> args, Collection<String> jvmArgs) {
        if (debug) {
            if (suspend) {
                args.add("-Dsuspend");
            }
            if (!LOCALHOST.equals(host)) {
                args.add("-DdebugHost=" + host);
            }
            if (mode == DebugMode.connect) {
                args.add("-Ddebug=client");
            }
            if (port != DEFAULT_PORT) {
                args.add("-DdebugPort=" + port);
            }
        } else {
            args.add("-Ddebug=false");
        }
    }

    @Override
    public String toString() {
        return "DebugOptions [debug=" + debug + ", mode=" + mode + ", host=" + host + ", port=" + port + ", suspend="
                + suspend + "]";
    }
}
