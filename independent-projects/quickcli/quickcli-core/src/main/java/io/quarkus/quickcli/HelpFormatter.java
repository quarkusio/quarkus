package io.quarkus.quickcli;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Map;

/**
 * Formats and prints help messages for commands.
 */
public final class HelpFormatter {

    private static final int OPTION_COLUMN_WIDTH = 30;

    private HelpFormatter() {
    }

    /**
     * Print help for the given command spec to the output stream.
     */
    public static void printHelp(CommandSpec spec, PrintStream out) {
        out.print(buildHelp(spec));
        out.flush();
    }

    /**
     * Print help for the given command spec to the writer.
     */
    public static void printHelp(CommandSpec spec, PrintWriter out) {
        out.print(buildHelp(spec));
        out.flush();
    }

    /**
     * Build the help text for the given command spec.
     */
    public static String buildHelp(CommandSpec spec) {
        StringBuilder out = new StringBuilder();

        // Header
        for (String line : spec.header()) {
            out.append(line).append('\n');
        }

        // Usage line
        out.append("Usage: ").append(spec.qualifiedName());
        if (!spec.options().isEmpty()) {
            out.append(" [OPTIONS]");
        }
        if (!spec.subcommands().isEmpty()) {
            out.append(" [COMMAND]");
        }
        for (ParameterSpec param : spec.parameters()) {
            if (!param.hidden()) {
                String label = param.paramLabel().isEmpty()
                        ? "<" + param.fieldName() + ">"
                        : param.paramLabel();
                out.append(' ').append(label);
            }
        }
        out.append('\n');

        // Description
        if (spec.description().length > 0) {
            out.append('\n');
            for (String line : spec.description()) {
                out.append("  ").append(line).append('\n');
            }
        }

        // Parameters
        boolean hasVisibleParams = spec.parameters().stream().anyMatch(p -> !p.hidden());
        if (hasVisibleParams) {
            out.append('\n');
            out.append("Parameters:\n");
            for (ParameterSpec param : spec.parameters()) {
                if (param.hidden())
                    continue;
                String label = param.paramLabel().isEmpty()
                        ? "<" + param.fieldName() + ">"
                        : param.paramLabel();
                appendDescriptionColumn(out, "  " + label, param.description());
            }
        }

        // Options
        boolean hasVisibleOptions = spec.options().stream().anyMatch(o -> !o.hidden());
        if (hasVisibleOptions || spec.mixinStandardHelpOptions()) {
            out.append('\n');
            out.append("Options:\n");
            for (OptionSpec option : spec.options()) {
                if (option.hidden())
                    continue;
                appendOption(out, option);
            }
            if (spec.mixinStandardHelpOptions()) {
                appendDescriptionColumn(out, "  -h, --help", "Show this help message and exit.");
                appendDescriptionColumn(out, "  -V, --version", "Print version information and exit.");
            }
        }

        // Subcommands
        if (!spec.subcommands().isEmpty()) {
            out.append('\n');
            out.append("Commands:\n");
            for (Map.Entry<String, CommandSpec> entry : spec.subcommands().entrySet()) {
                CommandSpec sub = entry.getValue();
                String desc = sub.description().length > 0 ? sub.description()[0] : "";
                appendDescriptionColumn(out, "  " + entry.getKey(), desc);
            }
        }

        // Footer
        if (spec.footer().length > 0) {
            out.append('\n');
            for (String line : spec.footer()) {
                out.append(line).append('\n');
            }
        }

        return out.toString();
    }

    private static void appendOption(StringBuilder out, OptionSpec option) {
        StringBuilder names = new StringBuilder("  ");
        String shortName = null;
        String longName = null;

        for (String name : option.names()) {
            if (name.startsWith("--")) {
                longName = name;
            } else {
                shortName = name;
            }
        }

        if (shortName != null) {
            names.append(shortName);
            if (longName != null) {
                names.append(", ").append(longName);
            }
        } else if (longName != null) {
            names.append("    ").append(longName);
        }

        if (!option.isBoolean()) {
            String label = option.paramLabel().isEmpty()
                    ? option.type().getSimpleName().toUpperCase()
                    : option.paramLabel();
            names.append("=<").append(label).append(">");
        }

        appendDescriptionColumn(out, names.toString(), option.description());
    }

    private static void appendDescriptionColumn(StringBuilder out, String left, String description) {
        if (left.length() >= OPTION_COLUMN_WIDTH) {
            out.append(left).append('\n');
            if (!description.isEmpty()) {
                out.append(" ".repeat(OPTION_COLUMN_WIDTH)).append(description).append('\n');
            }
        } else {
            out.append(left);
            if (!description.isEmpty()) {
                out.append(" ".repeat(OPTION_COLUMN_WIDTH - left.length()));
                out.append(description);
            }
            out.append('\n');
        }
    }
}
