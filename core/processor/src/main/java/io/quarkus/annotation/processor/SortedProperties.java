package io.quarkus.annotation.processor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.TreeSet;

/*
 * Please keep in sync with io.quarkus.bootstrap.util.SortedProperties
 */

/**
 * A subclass of {@link Properties} that guarantees binary reproducible storing of the entries into a property file.
 * In particular, this includes:
 * <ul>
 * <li>Alphabetic ordering of the entries in the file</li>
 * <li>Always using {@code \n} as a line delimiter instead of {@code System.getProperty("line.separator")} used by
 * {@code java.util.Properties}</li>
 * <li>Omitting the date comment that {@code java.util.Properties} always writes into the file</li>
 * </ul>
 */
public class SortedProperties extends Properties {

    public static void store(Properties props, String comments, OutputStream out) throws IOException {
        SortedProperties sortedProperties = new SortedProperties(props, null);
        sortedProperties.store(out, comments);
    }

    public static void store(Properties props, String comments, Writer out) throws IOException {
        SortedProperties sortedProperties = new SortedProperties(props, null);
        sortedProperties.store(out, comments);
    }

    private static final long serialVersionUID = -7888678715193822738L;

    public SortedProperties() {
        super();
    }

    public SortedProperties(Properties delegate, Properties defaults) {
        super(defaults);
        this.putAll(delegate);
    }

    @Override
    public synchronized Enumeration<Object> keys() {
        return Collections.enumeration(new TreeSet<Object>(super.keySet()));
    }

    @Override
    public void store(OutputStream out, String comments) throws IOException {
        store(new OutputStreamWriter(out, StandardCharsets.ISO_8859_1), comments);
    }

    public void store(Writer out, String comments) throws IOException {
        final int commentNewLines = countNewLines(comments);
        final DateOmittingWriter writer = new DateOmittingWriter(out, commentNewLines);
        super.store(writer, comments);
    }

    static int countNewLines(String comments) {
        if (comments == null) {
            return 0;
        }
        int result = 0;
        final char[] chars = comments.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '\n') {
                result++;
            }
        }
        return result + 1;
    }

    static class DateOmittingWriter extends BufferedWriter {
        private final int dateLineIndex;
        private int lineIndex = 0;

        public DateOmittingWriter(Writer out, int dateLineIndex) {
            super(out);
            this.dateLineIndex = dateLineIndex;
        }

        @Override
        public void newLine() throws IOException {
            if (lineIndex != dateLineIndex) {
                write('\n');
            }
            lineIndex++;
        }

        @Override
        public void write(String str) throws IOException {
            if (lineIndex == dateLineIndex) {
                // ignore
                return;
            }
            super.write(str);
        }

    }

}
