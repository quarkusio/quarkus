package io.quarkus.bootstrap.resolver.maven.options;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.maven.cli.CLIManager;

public class BootstrapMavenOptionsParser {

    public static Map<String, Object> parse(String[] args) {
        final CommandLine cmdLine;
        try {
            cmdLine = new CLIManager().parse(args);
        } catch (ParseException e) {
            throw new IllegalStateException("Failed to parse Maven command line arguments", e);
        }

        final Map<String, Object> map = new HashMap<>();
        put(cmdLine, map, CLIManager.ALTERNATE_USER_SETTINGS);
        put(cmdLine, map, CLIManager.ALTERNATE_GLOBAL_SETTINGS);
        put(map, String.valueOf(CLIManager.ACTIVATE_PROFILES), cmdLine.getOptionValues(CLIManager.ACTIVATE_PROFILES));

        putBoolean(cmdLine, map, CLIManager.SUPRESS_SNAPSHOT_UPDATES);
        putBoolean(cmdLine, map, CLIManager.UPDATE_SNAPSHOTS);
        putBoolean(cmdLine, map, CLIManager.CHECKSUM_FAILURE_POLICY);
        putBoolean(cmdLine, map, CLIManager.CHECKSUM_WARNING_POLICY);

        return map;
    }

    private static void put(CommandLine cmdLine, Map<String, Object> map, char name) {
        put(map, String.valueOf(name), cmdLine.getOptionValue(name));
    }

    private static void put(CommandLine cmdLine, Map<String, Object> map, String name) {
        put(map, name, cmdLine.getOptionValue(name));
    }

    private static void putBoolean(CommandLine cmdLine, Map<String, Object> map, char name) {
        if(cmdLine.hasOption(name)) {
            map.put(String.valueOf(name), Boolean.TRUE.toString());
        }
    }

    private static void putBoolean(CommandLine cmdLine, Map<String, Object> map, String name) {
        if(cmdLine.hasOption(name)) {
            map.put(name, Boolean.TRUE.toString());
        }
    }

    private static void put(Map<String, Object> map, String name, final Object value) {
        if(value != null) {
            map.put(name, value);
        }
    }
}
