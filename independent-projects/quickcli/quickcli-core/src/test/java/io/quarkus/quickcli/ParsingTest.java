package io.quarkus.quickcli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.quarkus.quickcli.model.BuiltCommandModel;
import io.quarkus.quickcli.model.BuiltCommandModel.FieldKind;
import io.quarkus.quickcli.model.CommandModelRegistry;

class ParsingTest {

    // --- Simple command classes (plain POJOs) ---

    static class GreetCmd implements Runnable {
        String name;
        int count;
        boolean verbose;

        @Override
        public void run() {
        }
    }

    static class RequiredCmd implements Runnable {
        String name;

        @Override
        public void run() {
        }
    }

    static class MultiCmd implements Runnable {
        List<String> items;

        @Override
        public void run() {
        }
    }

    static class MapCmd implements Runnable {
        Map<String, String> props;

        @Override
        public void run() {
        }
    }

    static class OptionalCmd implements Runnable {
        Optional<String> name = Optional.empty();

        @Override
        public void run() {
        }
    }

    static class SetCmd implements Runnable {
        Set<String> tags;

        @Override
        public void run() {
        }
    }

    static class ArrayCmd implements Runnable {
        String[] files;

        @Override
        public void run() {
        }
    }

    static class ParamCmd implements Runnable {
        String file;
        int line;

        @Override
        public void run() {
        }
    }

    static class MultiParamCmd implements Runnable {
        List<String> files;

        @Override
        public void run() {
        }
    }

    static class UnmatchedCmd implements Runnable {
        List<String> unmatched;

        @Override
        public void run() {
        }
    }

    static class DefaultCmd implements Runnable {
        String format;

        @Override
        public void run() {
        }
    }

    static class NegatableCmd implements Runnable {
        boolean verbose;

        @Override
        public void run() {
        }
    }

    static class SplitCmd implements Runnable {
        List<String> items;

        @Override
        public void run() {
        }
    }

    static class ExitCmd implements Callable<Integer> {
        int code;

        @Override
        public Integer call() {
            return code;
        }
    }

    static class ExclusiveCmd implements Runnable {
        boolean json;
        boolean xml;

        @Override
        public void run() {
        }
    }

    static class TopCmd implements Runnable {
        @Override
        public void run() {
        }
    }

    static class SubCmd implements Runnable {
        String name;
        boolean ran;

        @Override
        public void run() {
            ran = true;
        }
    }

    static class PrefixCmd implements Runnable {
        String prop;

        @Override
        public void run() {
        }
    }

    static class HiddenCmd implements Runnable {
        String secret;

        @Override
        public void run() {
        }
    }

    // --- Helper to register a model ---

    private void registerModel(BuiltCommandModel model) {
        CommandModelRegistry.register(model);
    }

    // --- Tests ---

    @Test
    void parseLongOption() {
        var ref = new AtomicReference<String>();
        registerModel(TestModelHelper.builder(GreetCmd.class, GreetCmd::new)
                .name("greet")
                .addOption(TestModelHelper.option(
                        new String[] { "-n", "--name" }, "Name", String.class,
                        (inst, val) -> ((GreetCmd) inst).name = (String) val))
                .build());

        CommandLine cmd = new CommandLine(GreetCmd.class);
        ParseResult result = cmd.parse("--name", "Alice");
        assertEquals("Alice", result.getOptionValue("--name"));
    }

    @Test
    void parseShortOption() {
        registerModel(TestModelHelper.builder(GreetCmd.class, GreetCmd::new)
                .name("greet")
                .addOption(TestModelHelper.option(
                        new String[] { "-n", "--name" }, "Name", String.class,
                        (inst, val) -> ((GreetCmd) inst).name = (String) val))
                .build());

        CommandLine cmd = new CommandLine(GreetCmd.class);
        ParseResult result = cmd.parse("-n", "Bob");
        assertEquals("Bob", result.getOptionValue("--name"));
    }

    @Test
    void parseInlineValue() {
        registerModel(TestModelHelper.builder(GreetCmd.class, GreetCmd::new)
                .name("greet")
                .addOption(TestModelHelper.option(
                        new String[] { "-n", "--name" }, "Name", String.class,
                        (inst, val) -> ((GreetCmd) inst).name = (String) val))
                .build());

        CommandLine cmd = new CommandLine(GreetCmd.class);
        ParseResult result = cmd.parse("--name=Charlie");
        assertEquals("Charlie", result.getOptionValue("--name"));
    }

    @Test
    void parseBooleanFlag() {
        registerModel(TestModelHelper.builder(GreetCmd.class, GreetCmd::new)
                .name("greet")
                .addOption(TestModelHelper.booleanOption(
                        new String[] { "-v", "--verbose" }, "Verbose",
                        (inst, val) -> ((GreetCmd) inst).verbose = (boolean) val))
                .build());

        CommandLine cmd = new CommandLine(GreetCmd.class);
        ParseResult result = cmd.parse("-v");
        assertEquals("true", result.getOptionValue("--verbose"));
    }

    @Test
    void parseBooleanFlagNotPresent() {
        registerModel(TestModelHelper.builder(GreetCmd.class, GreetCmd::new)
                .name("greet")
                .addOption(TestModelHelper.booleanOption(
                        new String[] { "-v", "--verbose" }, "Verbose",
                        (inst, val) -> ((GreetCmd) inst).verbose = (boolean) val))
                .build());

        CommandLine cmd = new CommandLine(GreetCmd.class);
        ParseResult result = cmd.parse();
        assertFalse(result.hasOption("--verbose"));
    }

    @Test
    void parseIntOption() {
        registerModel(TestModelHelper.builder(GreetCmd.class, GreetCmd::new)
                .name("greet")
                .addOption(TestModelHelper.option(
                        new String[] { "-c", "--count" }, "Count", int.class,
                        (inst, val) -> ((GreetCmd) inst).count = (int) val))
                .build());

        CommandLine cmd = new CommandLine(GreetCmd.class);
        int exitCode = cmd.execute("-c", "5");
        assertEquals(0, exitCode);
    }

    @Test
    void parseRequiredOptionMissing() {
        registerModel(TestModelHelper.builder(RequiredCmd.class, RequiredCmd::new)
                .name("req")
                .addOption(TestModelHelper.requiredOption(
                        new String[] { "--name" }, "Name", String.class,
                        (inst, val) -> ((RequiredCmd) inst).name = (String) val))
                .build());

        CommandLine cmd = new CommandLine(RequiredCmd.class);
        assertThrows(CommandLine.ParameterException.class, () -> cmd.parse());
    }

    @Test
    void parseRequiredOptionPresent() {
        registerModel(TestModelHelper.builder(RequiredCmd.class, RequiredCmd::new)
                .name("req")
                .addOption(TestModelHelper.requiredOption(
                        new String[] { "--name" }, "Name", String.class,
                        (inst, val) -> ((RequiredCmd) inst).name = (String) val))
                .build());

        CommandLine cmd = new CommandLine(RequiredCmd.class);
        ParseResult result = cmd.parse("--name", "test");
        assertEquals("test", result.getOptionValue("--name"));
    }

    @Test
    void unknownOptionThrows() {
        registerModel(TestModelHelper.builder(GreetCmd.class, GreetCmd::new)
                .name("greet")
                .build());

        CommandLine cmd = new CommandLine(GreetCmd.class);
        assertThrows(CommandLine.ParameterException.class, () -> cmd.parse("--unknown"));
    }

    @Test
    void optionMissingValueThrows() {
        registerModel(TestModelHelper.builder(GreetCmd.class, GreetCmd::new)
                .name("greet")
                .addOption(TestModelHelper.option(
                        new String[] { "--name" }, "Name", String.class,
                        (inst, val) -> ((GreetCmd) inst).name = (String) val))
                .build());

        CommandLine cmd = new CommandLine(GreetCmd.class);
        assertThrows(CommandLine.ParameterException.class, () -> cmd.parse("--name"));
    }

    @Test
    void parsePositionalParameters() {
        registerModel(TestModelHelper.builder(ParamCmd.class, ParamCmd::new)
                .name("param")
                .addParameter(TestModelHelper.parameter(0, "File", String.class,
                        (inst, val) -> ((ParamCmd) inst).file = (String) val))
                .addParameter(TestModelHelper.parameter(1, "Line", int.class,
                        (inst, val) -> ((ParamCmd) inst).line = (int) val))
                .build());

        CommandLine cmd = new CommandLine(ParamCmd.class);
        ParseResult result = cmd.parse("test.txt", "42");
        assertEquals("test.txt", result.getPositionalValue(0));
        assertEquals("42", result.getPositionalValue(1));
    }

    @Test
    void parseMultiValueParameter() {
        registerModel(TestModelHelper.builder(MultiParamCmd.class, MultiParamCmd::new)
                .name("multi")
                .addParameter(TestModelHelper.multiValueParameter(0, "Files", String.class,
                        (inst, val) -> ((MultiParamCmd) inst).files = castList(val)))
                .build());

        CommandLine cmd = new CommandLine(MultiParamCmd.class);
        ParseResult result = cmd.parse("a.txt", "b.txt", "c.txt");
        assertEquals(List.of("a.txt", "b.txt", "c.txt"), result.positionalValues());
    }

    @Test
    void parseDoubleDashEndsOptions() {
        registerModel(TestModelHelper.builder(ParamCmd.class, ParamCmd::new)
                .name("param")
                .addParameter(TestModelHelper.parameter(0, "File", String.class,
                        (inst, val) -> ((ParamCmd) inst).file = (String) val))
                .build());

        CommandLine cmd = new CommandLine(ParamCmd.class);
        ParseResult result = cmd.parse("--", "--not-an-option");
        assertEquals("--not-an-option", result.getPositionalValue(0));
    }

    @Test
    void parseHelpFlag() {
        registerModel(TestModelHelper.builder(GreetCmd.class, GreetCmd::new)
                .name("greet")
                .mixinStandardHelpOptions(true)
                .build());

        CommandLine cmd = new CommandLine(GreetCmd.class);
        ParseResult result = cmd.parse("--help");
        assertTrue(result.isHelpRequested());
    }

    @Test
    void parseHelpShortFlag() {
        registerModel(TestModelHelper.builder(GreetCmd.class, GreetCmd::new)
                .name("greet")
                .build());

        CommandLine cmd = new CommandLine(GreetCmd.class);
        ParseResult result = cmd.parse("-h");
        assertTrue(result.isHelpRequested());
    }

    @Test
    void parseVersionFlag() {
        registerModel(TestModelHelper.builder(GreetCmd.class, GreetCmd::new)
                .name("greet")
                .version("1.0.0")
                .mixinStandardHelpOptions(true)
                .build());

        CommandLine cmd = new CommandLine(GreetCmd.class);
        ParseResult result = cmd.parse("--version");
        assertTrue(result.isVersionRequested());
    }

    @Test
    void parseListOption() {
        registerModel(TestModelHelper.builder(MultiCmd.class, MultiCmd::new)
                .name("multi")
                .addOption(TestModelHelper.listOption(
                        new String[] { "-i", "--item" }, "Items", String.class,
                        (inst, val) -> ((MultiCmd) inst).items = castList(val)))
                .build());

        CommandLine cmd = new CommandLine(MultiCmd.class);
        ParseResult result = cmd.parse("-i", "a", "-i", "b", "-i", "c");
        assertEquals(List.of("a", "b", "c"), result.getOptionValues("--item"));
    }

    @Test
    void parseSetOption() {
        registerModel(TestModelHelper.builder(SetCmd.class, SetCmd::new)
                .name("set")
                .addOption(TestModelHelper.setOption(
                        new String[] { "-t", "--tag" }, "Tags", String.class,
                        (inst, val) -> ((SetCmd) inst).tags = castSet(val)))
                .build());

        CommandLine cmd = new CommandLine(SetCmd.class);
        ParseResult result = cmd.parse("-t", "a", "-t", "b", "-t", "a");
        assertEquals(List.of("a", "b", "a"), result.getOptionValues("--tag"));
    }

    @Test
    void parseArrayOption() {
        registerModel(TestModelHelper.builder(ArrayCmd.class, ArrayCmd::new)
                .name("arr")
                .addOption(TestModelHelper.arrayOption(
                        new String[] { "-f", "--file" }, "Files", String.class,
                        (inst, val) -> ((ArrayCmd) inst).files = (String[]) val))
                .build());

        CommandLine cmd = new CommandLine(ArrayCmd.class);
        ParseResult result = cmd.parse("-f", "a.txt", "-f", "b.txt");
        assertEquals(List.of("a.txt", "b.txt"), result.getOptionValues("--file"));
    }

    @Test
    void parseMapOption() {
        registerModel(TestModelHelper.builder(MapCmd.class, MapCmd::new)
                .name("map")
                .addOption(TestModelHelper.mapOption(
                        new String[] { "-D" }, "Properties", "",
                        (inst, val) -> ((MapCmd) inst).props = castMap(val)))
                .build());

        CommandLine cmd = new CommandLine(MapCmd.class);
        ParseResult result = cmd.parse("-D", "key=value", "-D", "foo=bar");
        assertEquals(List.of("key=value", "foo=bar"), result.getOptionValues("-D"));
    }

    @Test
    void parseOptionalOption() {
        registerModel(TestModelHelper.builder(OptionalCmd.class, OptionalCmd::new)
                .name("opt")
                .addOption(TestModelHelper.optionalOption(
                        new String[] { "--name" }, "Name", String.class,
                        (inst, val) -> ((OptionalCmd) inst).name = castOptional(val)))
                .build());

        CommandLine cmd = new CommandLine(OptionalCmd.class);
        ParseResult result = cmd.parse("--name", "test");
        assertEquals("test", result.getOptionValue("--name"));
    }

    @Test
    void parseDefaultValue() {
        registerModel(TestModelHelper.builder(DefaultCmd.class, DefaultCmd::new)
                .name("def")
                .addOption(TestModelHelper.defaultOption(
                        new String[] { "--format" }, "Format", String.class, "json",
                        (inst, val) -> ((DefaultCmd) inst).format = (String) val))
                .build());

        CommandLine cmd = new CommandLine(DefaultCmd.class);
        int code = cmd.execute();
        assertEquals(0, code);
    }

    @Test
    void parseNegatableOption() {
        registerModel(TestModelHelper.builder(NegatableCmd.class, NegatableCmd::new)
                .name("neg")
                .addOption(TestModelHelper.negatableOption(
                        new String[] { "--verbose" }, "Verbose", "",
                        (inst, val) -> ((NegatableCmd) inst).verbose = (boolean) val))
                .build());

        CommandLine cmd = new CommandLine(NegatableCmd.class);
        ParseResult result = cmd.parse("--no-verbose");
        assertEquals("false", result.getOptionValue("--verbose"));
    }

    @Test
    void parseNegatableOptionPositive() {
        registerModel(TestModelHelper.builder(NegatableCmd.class, NegatableCmd::new)
                .name("neg")
                .addOption(TestModelHelper.negatableOption(
                        new String[] { "--verbose" }, "Verbose", "",
                        (inst, val) -> ((NegatableCmd) inst).verbose = (boolean) val))
                .build());

        CommandLine cmd = new CommandLine(NegatableCmd.class);
        ParseResult result = cmd.parse("--verbose");
        assertEquals("true", result.getOptionValue("--verbose"));
    }

    @Test
    void parseSplitOption() {
        registerModel(TestModelHelper.builder(SplitCmd.class, SplitCmd::new)
                .name("split")
                .addOption(TestModelHelper.splitOption(
                        new String[] { "--items" }, "Items", String.class, ",",
                        (inst, val) -> ((SplitCmd) inst).items = castList(val)))
                .build());

        CommandLine cmd = new CommandLine(SplitCmd.class);
        ParseResult result = cmd.parse("--items", "a,b,c");
        assertEquals(List.of("a,b,c"), result.getOptionValues("--items"));
    }

    @Test
    void parsePrefixOption() {
        registerModel(TestModelHelper.builder(PrefixCmd.class, PrefixCmd::new)
                .name("prefix")
                .addOption(TestModelHelper.option(
                        new String[] { "-D" }, "Property", String.class,
                        (inst, val) -> ((PrefixCmd) inst).prop = (String) val))
                .build());

        CommandLine cmd = new CommandLine(PrefixCmd.class);
        ParseResult result = cmd.parse("-Dkey=value");
        assertEquals("key=value", result.getOptionValue("-D"));
    }

    @Test
    void parseUnmatchedArgs() {
        registerModel(TestModelHelper.builder(UnmatchedCmd.class, UnmatchedCmd::new)
                .name("unmatched")
                .hasUnmatchedField(true)
                .unmatchedAccessor((inst, val) -> ((UnmatchedCmd) inst).unmatched = castList(val))
                .build());

        CommandLine cmd = new CommandLine(UnmatchedCmd.class);
        ParseResult result = cmd.parse("--unknown", "extra");
        assertEquals(List.of("--unknown", "extra"), result.unmatched());
    }

    @Test
    void parseMutuallyExclusive() {
        registerModel(TestModelHelper.builder(ExclusiveCmd.class, ExclusiveCmd::new)
                .name("excl")
                .addOption(TestModelHelper.booleanOption(
                        new String[] { "--json" }, "JSON output",
                        (inst, val) -> ((ExclusiveCmd) inst).json = (boolean) val))
                .addOption(TestModelHelper.booleanOption(
                        new String[] { "--xml" }, "XML output",
                        (inst, val) -> ((ExclusiveCmd) inst).xml = (boolean) val))
                .addExclusiveGroup(List.of("--json", "--xml"))
                .build());

        CommandLine cmd = new CommandLine(ExclusiveCmd.class);
        assertThrows(CommandLine.ParameterException.class, () ->
                cmd.parse("--json", "--xml"));
    }

    @Test
    void parseMutuallyExclusiveSingle() {
        registerModel(TestModelHelper.builder(ExclusiveCmd.class, ExclusiveCmd::new)
                .name("excl")
                .addOption(TestModelHelper.booleanOption(
                        new String[] { "--json" }, "JSON output",
                        (inst, val) -> ((ExclusiveCmd) inst).json = (boolean) val))
                .addOption(TestModelHelper.booleanOption(
                        new String[] { "--xml" }, "XML output",
                        (inst, val) -> ((ExclusiveCmd) inst).xml = (boolean) val))
                .addExclusiveGroup(List.of("--json", "--xml"))
                .build());

        CommandLine cmd = new CommandLine(ExclusiveCmd.class);
        ParseResult result = cmd.parse("--json");
        assertTrue(result.hasOption("--json"));
        assertFalse(result.hasOption("--xml"));
    }

    @Test
    void executeCallable() {
        registerModel(TestModelHelper.builder(ExitCmd.class, ExitCmd::new)
                .name("exit")
                .addOption(TestModelHelper.option(
                        new String[] { "--code" }, "Exit code", int.class,
                        (inst, val) -> ((ExitCmd) inst).code = (int) val))
                .build());

        CommandLine cmd = new CommandLine(ExitCmd.class);
        assertEquals(42, cmd.execute("--code", "42"));
    }

    @Test
    void executeRunnable() {
        registerModel(TestModelHelper.builder(GreetCmd.class, GreetCmd::new)
                .name("greet")
                .build());

        CommandLine cmd = new CommandLine(GreetCmd.class);
        assertEquals(0, cmd.execute());
    }

    @Test
    void executeHelp() {
        registerModel(TestModelHelper.builder(GreetCmd.class, GreetCmd::new)
                .name("greet")
                .description("A greeting command")
                .mixinStandardHelpOptions(true)
                .build());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CommandLine cmd = new CommandLine(GreetCmd.class);
        cmd.setOut(new PrintStream(baos));
        int code = cmd.execute("--help");
        assertEquals(0, code);
        assertTrue(baos.toString().contains("greet"));
    }

    @Test
    void executeVersion() {
        registerModel(TestModelHelper.builder(GreetCmd.class, GreetCmd::new)
                .name("greet")
                .version("2.0.0")
                .mixinStandardHelpOptions(true)
                .build());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CommandLine cmd = new CommandLine(GreetCmd.class);
        cmd.setOut(new PrintStream(baos));
        int code = cmd.execute("--version");
        assertEquals(0, code);
        assertTrue(baos.toString().contains("2.0.0"));
    }

    @Test
    void executeWithError() {
        registerModel(TestModelHelper.builder(GreetCmd.class, GreetCmd::new)
                .name("greet")
                .build());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CommandLine cmd = new CommandLine(GreetCmd.class);
        cmd.setErr(new PrintStream(baos));
        int code = cmd.execute("--unknown");
        assertEquals(ExitCode.USAGE, code);
        assertTrue(baos.toString().contains("Error"));
    }

    @Test
    void customParameterExceptionHandler() {
        registerModel(TestModelHelper.builder(GreetCmd.class, GreetCmd::new)
                .name("greet")
                .build());

        CommandLine cmd = new CommandLine(GreetCmd.class);
        cmd.setParameterExceptionHandler((ex, args) -> 99);
        assertEquals(99, cmd.execute("--unknown"));
    }

    @Test
    void parseSubcommand() {
        registerModel(TestModelHelper.builder(SubCmd.class, SubCmd::new)
                .name("sub")
                .addOption(TestModelHelper.option(
                        new String[] { "--name" }, "Name", String.class,
                        (inst, val) -> ((SubCmd) inst).name = (String) val))
                .build());
        registerModel(TestModelHelper.builder(TopCmd.class, TopCmd::new)
                .name("top")
                .addSubcommand(SubCmd.class)
                .build());

        CommandLine cmd = new CommandLine(TopCmd.class);
        ParseResult result = cmd.parse("sub", "--name", "test");
        assertNotNull(result.subcommandResult());
        assertEquals("test", result.subcommandResult().getOptionValue("--name"));
    }

    @Test
    void multipleOptions() {
        registerModel(TestModelHelper.builder(GreetCmd.class, GreetCmd::new)
                .name("greet")
                .addOption(TestModelHelper.option(
                        new String[] { "-n", "--name" }, "Name", String.class,
                        (inst, val) -> ((GreetCmd) inst).name = (String) val))
                .addOption(TestModelHelper.option(
                        new String[] { "-c", "--count" }, "Count", int.class,
                        (inst, val) -> ((GreetCmd) inst).count = (int) val))
                .addOption(TestModelHelper.booleanOption(
                        new String[] { "-v", "--verbose" }, "Verbose",
                        (inst, val) -> ((GreetCmd) inst).verbose = (boolean) val))
                .build());

        CommandLine cmd = new CommandLine(GreetCmd.class);
        ParseResult result = cmd.parse("-n", "Alice", "-c", "3", "-v");
        assertEquals("Alice", result.getOptionValue("--name"));
        assertEquals("3", result.getOptionValue("--count"));
        assertEquals("true", result.getOptionValue("--verbose"));
    }

    @Test
    void emptyArgs() {
        registerModel(TestModelHelper.builder(GreetCmd.class, GreetCmd::new)
                .name("greet")
                .build());

        CommandLine cmd = new CommandLine(GreetCmd.class);
        ParseResult result = cmd.parse();
        assertNotNull(result);
        assertTrue(result.positionalValues().isEmpty());
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> castList(Object val) {
        return (List<T>) val;
    }

    @SuppressWarnings("unchecked")
    private static <T> Set<T> castSet(Object val) {
        return (Set<T>) val;
    }

    @SuppressWarnings("unchecked")
    private static <K, V> Map<K, V> castMap(Object val) {
        return (Map<K, V>) val;
    }

    @SuppressWarnings("unchecked")
    private static <T> Optional<T> castOptional(Object val) {
        return (Optional<T>) val;
    }
}
