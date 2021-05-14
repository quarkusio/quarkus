package io.quarkus.cli.common;

import picocli.CommandLine;

public class DebugOptions {
    public enum DebugMode {
        connect,
        listen
    }

    @CommandLine.Option(order = 7, names = {
            "--no-debug" }, description = "Toggle debug mode. Enabled by default.", negatable = true)
    public boolean debug = true;

    @CommandLine.Option(order = 8, names = {
            "--debug-host" }, description = "Debug host, e.g. localhost or 0.0.0.0", defaultValue = "localhost")
    public String host = "localhost";

    @CommandLine.Option(order = 9, names = {
            "--debug-mode" }, description = "Valid values: ${COMPLETION-CANDIDATES}.%nEither connect to or listen on <host>:<port>.", defaultValue = "listen")
    public DebugMode mode = DebugMode.listen;

    @CommandLine.Option(order = 10, names = {
            "--debug-port" }, description = "Debug port.", defaultValue = "5005")
    public int port = 5005;

    @CommandLine.Option(order = 11, names = {
            "--suspend" }, description = "In listen mode, suspend until a debugger is attached. Disabled by default.", negatable = true)
    public boolean suspend = false;

    public String getJdwpArgument() {
        return "-Xrunjdwp:transport=dt_socket,address="
                + host + ":" + port
                + ",server=" + (mode == DebugMode.listen ? "y" : "n")
                + ",suspend=" + suspend;
    }

    @Override
    public String toString() {
        return "DebugOptions [debug=" + debug + ", mode=" + mode + ", host=" + host + ", port=" + port + ", suspend="
                + suspend + "]";
    }
}
