package io.quarkus.bootstrap.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 *
 * @author Alexey Loubyansky
 */
public class PropertyUtils {

    private static final String OS_NAME = "os.name";
    private static final String USER_HOME = "user.home";
    private static final String WINDOWS = "windows";

    private static final String FALSE = "false";
    private static final String TRUE = "true";

    private PropertyUtils() {
    }

    public static boolean isWindows() {
        return getProperty(OS_NAME).toLowerCase(Locale.ENGLISH).contains(WINDOWS);
    }

    public static String getUserHome() {
        return getProperty(USER_HOME);
    }

    public static String getProperty(final String name, String defValue) {
        assert name != null : "name is null";
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            return AccessController.doPrivileged(new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return System.getProperty(name, defValue);
                }
            });
        } else {
            return System.getProperty(name, defValue);
        }
    }

    public static String getProperty(final String name) {
        assert name != null : "name is null";
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            return AccessController.doPrivileged(new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return System.getProperty(name);
                }
            });
        } else {
            return System.getProperty(name);
        }
    }

    public static final Boolean getBooleanOrNull(String name) {
        final String value = getProperty(name);
        return value == null ? null : Boolean.parseBoolean(value);
    }

    public static final boolean getBoolean(String name, boolean notFoundValue) {
        final String value = getProperty(name, (notFoundValue ? TRUE : FALSE));
        return value.isEmpty() ? true : Boolean.parseBoolean(value);
    }

    /**
     * Stores properties into a file sorting the keys alphabetically and following
     * {@link Properties#store(Writer, String)} format but skipping the timestamp and comments.
     *
     * @param properties properties to store
     * @param file target file
     * @throws IOException in case of a failure
     */
    public static void store(Properties properties, Path file) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            store(properties, writer);
        }
    }

    /**
     * Stores properties into a file sorting the keys alphabetically and following
     * {@link Properties#store(Writer, String)} format but skipping the timestamp and comments.
     *
     * @param properties properties to store
     * @param writer target writer
     * @throws IOException in case of a failure
     */
    public static void store(Properties properties, Writer writer) throws IOException {
        final List<String> names = new ArrayList<>(properties.size());
        for (var name : properties.keySet()) {
            names.add(name == null ? null : name.toString());
        }
        Collections.sort(names);
        for (String name : names) {
            store(writer, name, properties.getProperty(name));
        }
    }

    /**
     * Stores a map of strings into a file sorting the keys alphabetically and following
     * {@link Properties#store(Writer, String)} format but skipping the timestamp and comments.
     *
     * @param properties properties to store
     * @param file target file
     * @throws IOException in case of a failure
     */
    public static void store(Map<String, String> properties, Path file) throws IOException {
        final List<String> names = new ArrayList<>(properties.keySet());
        Collections.sort(names);
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            for (String name : names) {
                store(writer, name, properties.get(name));
            }
        }
    }

    /**
     * Writes a config option with its value to the target writer,
     * possibly applying some transformations, such as character escaping
     * prior to writing.
     *
     * @param writer target writer
     * @param name option name
     * @param value option value
     * @throws IOException in case of a failure
     */
    public static void store(Writer writer, String name, String value) throws IOException {
        if (value != null) {
            name = toWritableValue(name, true, true);
            value = toWritableValue(value, false, true);
            writer.write(name);
            writer.write("=");
            writer.write(value);
            writer.write(System.lineSeparator());
        }
    }

    /**
     * Escapes characters that are expected to be escaped when {@link java.util.Properties} load
     * files from disk.
     *
     * @param str property name or value
     * @param escapeSpace whether to escape a whitespace (should be true for property names)
     * @param escapeUnicode whether to converts unicodes to encoded &#92;uxxxx
     * @return property name or value that can be written to a file
     */
    private static String toWritableValue(String str, boolean escapeSpace, boolean escapeUnicode) {
        int len = str.length();
        int bufLen = len * 2;
        if (bufLen < 0) {
            bufLen = Integer.MAX_VALUE;
        }
        StringBuilder outBuffer = new StringBuilder(bufLen);

        for (int x = 0; x < len; x++) {
            char aChar = str.charAt(x);
            // Handle common case first, selecting largest block that
            // avoids the specials below
            if ((aChar > 61) && (aChar < 127)) {
                if (aChar == '\\') {
                    outBuffer.append('\\');
                    outBuffer.append('\\');
                    continue;
                }
                outBuffer.append(aChar);
                continue;
            }
            switch (aChar) {
                case ' ':
                    if (x == 0 || escapeSpace) {
                        outBuffer.append('\\');
                    }
                    outBuffer.append(' ');
                    break;
                case '\t':
                    outBuffer.append('\\');
                    outBuffer.append('t');
                    break;
                case '\n':
                    outBuffer.append('\\');
                    outBuffer.append('n');
                    break;
                case '\r':
                    outBuffer.append('\\');
                    outBuffer.append('r');
                    break;
                case '\f':
                    outBuffer.append('\\');
                    outBuffer.append('f');
                    break;
                case '=': // Fall through
                case ':': // Fall through
                case '#': // Fall through
                case '!':
                    outBuffer.append('\\');
                    outBuffer.append(aChar);
                    break;
                default:
                    if (((aChar < 0x0020) || (aChar > 0x007e)) & escapeUnicode) {
                        outBuffer.append('\\');
                        outBuffer.append('u');
                        outBuffer.append(toHex((aChar >> 12) & 0xF));
                        outBuffer.append(toHex((aChar >> 8) & 0xF));
                        outBuffer.append(toHex((aChar >> 4) & 0xF));
                        outBuffer.append(toHex(aChar & 0xF));
                    } else {
                        outBuffer.append(aChar);
                    }
            }
        }
        return outBuffer.toString();
    }

    /**
     * Convert a nibble to a hex character
     *
     * @param nibble the nibble to convert.
     */
    private static char toHex(int nibble) {
        return hexDigit[(nibble & 0xF)];
    }

    /** A table of hex digits */
    private static final char[] hexDigit = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };
}
