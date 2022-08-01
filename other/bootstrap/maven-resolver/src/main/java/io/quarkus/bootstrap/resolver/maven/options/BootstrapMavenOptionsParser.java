package io.quarkus.bootstrap.resolver.maven.options;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.maven.cli.CLIManager;

public class BootstrapMavenOptionsParser {

    public static Map<String, Object> parse(String[] args) {

        List<String> argsWoProps = null;

        // the CLIManager doesn't seem to properly parse -D properties, it's missing the value separator config option
        // here we are separating the user properties from the rest and passing only the non-system property args to the CLIManager
        Properties userProps = null;
        int argI = 0;
        while (argI < args.length) {
            final String arg = args[argI++];
            if (!arg.startsWith("-D")) {
                if (argsWoProps != null) {
                    argsWoProps.add(arg);
                }
                continue;
            }

            if (argsWoProps == null) {
                argsWoProps = new ArrayList<>(args.length);
                for (int i = 0; i < argI - 1; ++i) {
                    argsWoProps.add(args[i]);
                }
            }

            if (userProps == null) {
                userProps = new Properties();
            }
            final String expr;
            if (arg.length() == 2) {
                // it means there was a space after -D
                if (argI == args.length) {
                    break;
                }
                expr = args[argI++];
            } else {
                expr = arg.substring(2);
            }

            int equals = expr.indexOf('=');
            String name;
            String parsedValue;
            if (equals < 0) {
                name = expr;
                parsedValue = null;
            } else {
                name = expr.substring(0, equals);
                parsedValue = expr.substring(equals + 1);
            }

            final String systemValue = System.getProperty(name);
            if (systemValue == null) {
                // the property could be coming from MAVEN_OPTS
                userProps.setProperty(name, parsedValue == null ? "true" : parsedValue);
            } else {
                userProps.setProperty(name, systemValue);
                if (parsedValue != null && !systemValue.equals(parsedValue)) {
                    // this could be a value with a whitespace, in which case it will appear broken into tokens
                    int valueI = 0;
                    int parsedI = argI;
                    while (valueI < systemValue.length()) {
                        if (Character.isWhitespace(systemValue.charAt(valueI))) {
                            ++valueI;
                            continue;
                        }
                        if (!systemValue.startsWith(parsedValue, valueI)) {
                            break;
                        }
                        argI = parsedI;
                        valueI += parsedValue.length();
                        if (parsedI < args.length) {
                            parsedValue = args[parsedI++];
                        } else {
                            break;
                        }
                    }
                }
            }
        }

        if (argsWoProps != null) {
            args = argsWoProps.toArray(new String[0]);
        }

        final CommandLine cmdLine;
        try {
            cmdLine = new CLIManager().parse(args);
        } catch (ParseException e) {
            throw new IllegalStateException("Failed to parse Maven command line arguments", e);
        }

        final Map<String, Object> map = new HashMap<>();
        put(cmdLine, map, CLIManager.ALTERNATE_USER_SETTINGS);
        put(cmdLine, map, CLIManager.ALTERNATE_GLOBAL_SETTINGS);
        put(cmdLine, map, CLIManager.ALTERNATE_POM_FILE);
        put(map, String.valueOf(CLIManager.ACTIVATE_PROFILES), cmdLine.getOptionValues(CLIManager.ACTIVATE_PROFILES));

        putBoolean(cmdLine, map, CLIManager.OFFLINE);
        putBoolean(cmdLine, map, CLIManager.SUPRESS_SNAPSHOT_UPDATES);
        putBoolean(cmdLine, map, CLIManager.UPDATE_SNAPSHOTS);
        putBoolean(cmdLine, map, CLIManager.CHECKSUM_FAILURE_POLICY);
        putBoolean(cmdLine, map, CLIManager.CHECKSUM_WARNING_POLICY);
        putBoolean(cmdLine, map, CLIManager.BATCH_MODE);
        putBoolean(cmdLine, map, CLIManager.NO_TRANSFER_PROGRESS);

        if (userProps != null) {
            put(map, String.valueOf(CLIManager.SET_SYSTEM_PROPERTY), userProps);
        }
        return map;
    }

    private static void put(CommandLine cmdLine, Map<String, Object> map, char name) {
        put(map, String.valueOf(name), cmdLine.getOptionValue(name));
    }

    private static void put(CommandLine cmdLine, Map<String, Object> map, String name) {
        put(map, name, cmdLine.getOptionValue(name));
    }

    private static void putBoolean(CommandLine cmdLine, Map<String, Object> map, char name) {
        if (cmdLine.hasOption(name)) {
            map.put(String.valueOf(name), Boolean.TRUE.toString());
        }
    }

    private static void putBoolean(CommandLine cmdLine, Map<String, Object> map, String name) {
        if (cmdLine.hasOption(name)) {
            map.put(name, Boolean.TRUE.toString());
        }
    }

    private static void put(Map<String, Object> map, String name, final Object value) {
        if (value != null) {
            map.put(name, value);
        }
    }
}
