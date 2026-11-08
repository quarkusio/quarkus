package io.quarkus.cli;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import io.quarkus.cli.common.OutputOptionMixin;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParseResult;

/**
 * Forwards the arguments of a command that accepts unmatched arguments to its default subcommand, e.g.
 * {@code quarkus create my-app} to {@code quarkus create app my-app}.
 * <p>
 * A first positional argument that looks like a mistyped subcommand, i.e. one that the default subcommand
 * does not accept or that is close to the name of a subcommand, is reported as an unknown subcommand with
 * suggestions instead of being forwarded, so that the user learns which subcommand was meant.
 */
final class DefaultSubcommand {

    private DefaultSubcommand() {
    }

    /**
     * @param spec the spec of the command that forwards
     * @param output the output of the command
     * @param defaultSubcommand the name of the subcommand to forward to
     * @param message the message printed before forwarding, or {@code null}
     * @return the exit code
     */
    static int forward(CommandSpec spec, OutputOptionMixin output, String defaultSubcommand, String message) {
        ParseResult parseResult = spec.commandLine().getParseResult();
        List<String> args = withoutCommandName(parseResult.originalArgs(), spec);
        CommandLine target = spec.subcommands().get(defaultSubcommand);

        String firstPositional = firstPositional(args);
        if (firstPositional != null) {
            Set<String> names = subcommandNames(spec);
            List<String> suggestions = similarNames(firstPositional, names);
            if (!suggestions.isEmpty() || isRejected(target, args, firstPositional)) {
                output.error("Unknown subcommand '" + firstPositional + "' for '" + spec.qualifiedName() + "'.");
                if (!suggestions.isEmpty()) {
                    output.info("Did you mean: " + String.join(", ", suggestions) + "?");
                }
                output.info("Available subcommands: " + String.join(", ", names) + ". See '" + spec.qualifiedName()
                        + " --help'.");
                return CommandLine.ExitCode.USAGE;
            }
        }

        if (message != null) {
            output.info(message);
        }
        return target.execute(args.toArray(new String[0]));
    }

    /**
     * Removes the first occurrence of the name or an alias of the command, which is the command itself; a later
     * occurrence is an argument, e.g. the artifact id in {@code quarkus create create}.
     */
    private static List<String> withoutCommandName(List<String> originalArgs, CommandSpec spec) {
        List<String> args = new ArrayList<>(originalArgs);
        Set<String> names = new LinkedHashSet<>();
        names.add(spec.name());
        names.addAll(List.of(spec.aliases()));
        for (int i = 0; i < args.size(); i++) {
            if (names.contains(args.get(i))) {
                args.remove(i);
                break;
            }
        }
        return args;
    }

    private static String firstPositional(List<String> args) {
        for (String arg : args) {
            if ("--".equals(arg)) {
                return null;
            }
            if (!arg.startsWith("-")) {
                return arg;
            }
        }
        return null;
    }

    private static Set<String> subcommandNames(CommandSpec spec) {
        Set<String> names = new TreeSet<>();
        for (CommandLine subcommand : spec.subcommands().values()) {
            names.add(subcommand.getCommandName());
        }
        return names;
    }

    /**
     * @return {@code true} if the default subcommand reports the first positional argument as unmatched
     */
    private static boolean isRejected(CommandLine target, List<String> args, String firstPositional) {
        try {
            ParseResult dryRun = target.parseArgs(args.toArray(new String[0]));
            return dryRun.unmatched().contains(firstPositional);
        } catch (CommandLine.ParameterException e) {
            return false;
        }
    }

    /**
     * @return the subcommand names the argument is a prefix of, or is within a small edit distance of
     */
    static List<String> similarNames(String argument, Set<String> names) {
        List<String> similar = new ArrayList<>();
        String candidate = argument.toLowerCase(Locale.ROOT);
        for (String name : names) {
            if (candidate.length() >= 2 && name.startsWith(candidate)) {
                similar.add(name);
            } else if (candidate.length() >= 3 && name.length() >= 3) {
                int maxDistance = candidate.length() >= 4 && name.length() >= 4 ? 2 : 1;
                if (editDistance(candidate, name) <= maxDistance) {
                    similar.add(name);
                }
            }
        }
        return similar;
    }

    private static int editDistance(String a, String b) {
        int[] previous = new int[b.length() + 1];
        int[] current = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int substitution = previous[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1);
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), substitution);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[b.length()];
    }
}
