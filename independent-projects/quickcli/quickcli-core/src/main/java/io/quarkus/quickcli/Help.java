package io.quarkus.quickcli;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Provides help rendering for commands. Supports customizable column layouts
 * and hierarchical subcommand display.
 */
public final class Help {

    private final CommandSpec spec;
    private final ColorScheme colorScheme;

    public Help(CommandSpec spec) {
        this(spec, ColorScheme.DEFAULT);
    }

    public Help(CommandSpec spec, ColorScheme colorScheme) {
        this.spec = spec;
        this.colorScheme = colorScheme;
    }

    public CommandSpec commandSpec() {
        return spec;
    }

    public ColorScheme colorScheme() {
        return colorScheme;
    }

    /** Returns the full synopsis (usage line) for this command. */
    public String fullSynopsis() {
        StringBuilder sb = new StringBuilder();
        UsageMessageSpec usage = spec.usageMessage();
        sb.append(formatHeading(usage.synopsisHeading()));
        sb.append(spec.qualifiedName());
        if (!spec.options().isEmpty()) {
            sb.append(" [OPTIONS]");
        }
        if (!spec.subcommands().isEmpty()) {
            sb.append(" [COMMAND]");
        }
        for (ParameterSpec param : spec.parameters()) {
            if (!param.hidden()) {
                String label = param.paramLabel().isEmpty()
                        ? "<" + param.fieldName() + ">"
                        : param.paramLabel();
                sb.append(" ").append(label);
            }
        }
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    /** Returns formatted command list section. */
    public String commandList() {
        if (spec.subcommands().isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        UsageMessageSpec usage = spec.usageMessage();
        sb.append(formatHeading(usage.commandListHeading()));
        for (Map.Entry<String, CommandSpec> entry : spec.subcommands().entrySet()) {
            CommandSpec sub = entry.getValue();
            String desc = sub.description().length > 0 ? sub.description()[0] : "";
            sb.append(String.format("  %-22s %s%n", entry.getKey(), desc));
        }
        return sb.toString();
    }

    /** Format a heading string, replacing %n with newlines. */
    static String formatHeading(String heading) {
        if (heading == null || heading.isEmpty()) {
            return "";
        }
        return heading.replace("%n", System.lineSeparator());
    }

    // --- Column and TextTable ---

    /** Defines a column in a text table. */
    public static class Column {
        public final int width;
        public final int indent;
        public final Overflow overflow;

        public Column(int width, int indent, Overflow overflow) {
            this.width = width;
            this.indent = indent;
            this.overflow = overflow;
        }

        public enum Overflow {
            SPAN, WRAP, TRUNCATE
        }
    }

    /** A simple text table for rendering aligned columns. */
    public static class TextTable {
        private final List<String[]> rows = new ArrayList<>();
        private final Column[] columns;
        private boolean adjustLineBreaksForWideCJKCharacters;

        private TextTable(Column... columns) {
            this.columns = columns;
        }

        public static TextTable forColumns(ColorScheme colorScheme, Column... columns) {
            return new TextTable(columns);
        }

        public void setAdjustLineBreaksForWideCJKCharacters(boolean adjust) {
            this.adjustLineBreaksForWideCJKCharacters = adjust;
        }

        public void addRowValues(String... values) {
            rows.add(values);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (String[] row : rows) {
                StringBuilder line = new StringBuilder();
                for (int i = 0; i < columns.length && i < row.length; i++) {
                    String value = row[i] != null ? row[i] : "";
                    Column col = columns[i];
                    String indent = " ".repeat(col.indent);
                    if (i == 0) {
                        line.append(indent).append(value);
                        int padding = col.width - indent.length() - value.length();
                        if (padding > 0) {
                            line.append(" ".repeat(padding));
                        } else if (i + 1 < columns.length) {
                            line.append(System.lineSeparator());
                            line.append(" ".repeat(col.width));
                        }
                    } else {
                        line.append(indent).append(value);
                    }
                }
                sb.append(line.toString().stripTrailing()).append(System.lineSeparator());
            }
            return sb.toString();
        }
    }

    /** Color scheme for help output. Provides picocli-compatible API. */
    public static class ColorScheme {
        public static final ColorScheme DEFAULT = new ColorScheme();

        public Ansi ansi() {
            return Ansi.AUTO;
        }

        /** Returns plain text with markup stripped. */
        public String errorText(String text) {
            return Ansi.Text.stripAnsiMarkup(text);
        }

        /** Returns a plain-text rendering of a stack trace. */
        public String stackTraceText(Exception ex) {
            java.io.StringWriter sw = new java.io.StringWriter();
            ex.printStackTrace(new java.io.PrintWriter(sw));
            return sw.toString();
        }
    }

    /** Creates a formatted text table from a map (key-value pairs). */
    public String createTextTable(java.util.Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        for (java.util.Map.Entry<String, String> entry : map.entrySet()) {
            sb.append(String.format("  %-22s %s%n", entry.getKey(), entry.getValue()));
        }
        return sb.toString();
    }
}
