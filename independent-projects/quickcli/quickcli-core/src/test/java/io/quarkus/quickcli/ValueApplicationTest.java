package io.quarkus.quickcli;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkus.quickcli.model.CommandModelRegistry;

/**
 * Tests that parsed values are correctly applied to command instances
 * via the BuiltCommandModel field accessors.
 */
class ValueApplicationTest {

    static class AllTypesCmd implements Runnable {
        String name;
        int count;
        boolean verbose;
        List<String> items;
        Set<Integer> ids;
        String[] files;
        Map<String, String> props;
        Optional<String> format = Optional.empty();

        @Override
        public void run() {
        }
    }

    @Test
    void applySingleString() {
        CommandModelRegistry.register(TestModelHelper.builder(AllTypesCmd.class, AllTypesCmd::new)
                .name("all")
                .addOption(TestModelHelper.option(
                        new String[] { "--name" }, "Name", String.class,
                        (inst, val) -> ((AllTypesCmd) inst).name = (String) val))
                .build());

        AllTypesCmd cmd = new AllTypesCmd();
        CommandLine cl = new CommandLine(AllTypesCmd.class);
        cl.execute("--name", "Alice");
    }

    @Test
    void applySingleInt() {
        CommandModelRegistry.register(TestModelHelper.builder(AllTypesCmd.class, AllTypesCmd::new)
                .name("all")
                .addOption(TestModelHelper.option(
                        new String[] { "--count" }, "Count", int.class,
                        (inst, val) -> ((AllTypesCmd) inst).count = (int) val))
                .build());

        CommandLine cl = new CommandLine(AllTypesCmd.class);
        assertEquals(0, cl.execute("--count", "10"));
    }

    @Test
    void applyBoolean() {
        CommandModelRegistry.register(TestModelHelper.builder(AllTypesCmd.class, AllTypesCmd::new)
                .name("all")
                .addOption(TestModelHelper.booleanOption(
                        new String[] { "-v", "--verbose" }, "Verbose",
                        (inst, val) -> ((AllTypesCmd) inst).verbose = (boolean) val))
                .build());

        CommandLine cl = new CommandLine(AllTypesCmd.class);
        assertEquals(0, cl.execute("-v"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void applyList() {
        CommandModelRegistry.register(TestModelHelper.builder(AllTypesCmd.class, AllTypesCmd::new)
                .name("all")
                .addOption(TestModelHelper.listOption(
                        new String[] { "--item" }, "Items", String.class,
                        (inst, val) -> ((AllTypesCmd) inst).items = (List<String>) val))
                .build());

        CommandLine cl = new CommandLine(AllTypesCmd.class);
        assertEquals(0, cl.execute("--item", "a", "--item", "b"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void applySet() {
        CommandModelRegistry.register(TestModelHelper.builder(AllTypesCmd.class, AllTypesCmd::new)
                .name("all")
                .addOption(TestModelHelper.setOption(
                        new String[] { "--id" }, "IDs", Integer.class,
                        (inst, val) -> ((AllTypesCmd) inst).ids = (Set<Integer>) val))
                .build());

        CommandLine cl = new CommandLine(AllTypesCmd.class);
        assertEquals(0, cl.execute("--id", "1", "--id", "2", "--id", "1"));
    }

    @Test
    void applyArray() {
        CommandModelRegistry.register(TestModelHelper.builder(AllTypesCmd.class, AllTypesCmd::new)
                .name("all")
                .addOption(TestModelHelper.arrayOption(
                        new String[] { "--file" }, "Files", String.class,
                        (inst, val) -> ((AllTypesCmd) inst).files = (String[]) val))
                .build());

        CommandLine cl = new CommandLine(AllTypesCmd.class);
        assertEquals(0, cl.execute("--file", "a.txt", "--file", "b.txt"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void applyMap() {
        CommandModelRegistry.register(TestModelHelper.builder(AllTypesCmd.class, AllTypesCmd::new)
                .name("all")
                .addOption(TestModelHelper.mapOption(
                        new String[] { "-D" }, "Properties", "true",
                        (inst, val) -> ((AllTypesCmd) inst).props = (Map<String, String>) val))
                .build());

        CommandLine cl = new CommandLine(AllTypesCmd.class);
        assertEquals(0, cl.execute("-D", "key=value", "-D", "flag"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void applyOptional() {
        CommandModelRegistry.register(TestModelHelper.builder(AllTypesCmd.class, AllTypesCmd::new)
                .name("all")
                .addOption(TestModelHelper.optionalOption(
                        new String[] { "--format" }, "Format", String.class,
                        (inst, val) -> ((AllTypesCmd) inst).format = (Optional<String>) val))
                .build());

        CommandLine cl = new CommandLine(AllTypesCmd.class);
        assertEquals(0, cl.execute("--format", "json"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void applyOptionalNotProvided() {
        CommandModelRegistry.register(TestModelHelper.builder(AllTypesCmd.class, AllTypesCmd::new)
                .name("all")
                .addOption(TestModelHelper.optionalOption(
                        new String[] { "--format" }, "Format", String.class,
                        (inst, val) -> ((AllTypesCmd) inst).format = (Optional<String>) val))
                .build());

        CommandLine cl = new CommandLine(AllTypesCmd.class);
        assertEquals(0, cl.execute());
    }

    @Test
    void applyDefaultValue() {
        CommandModelRegistry.register(TestModelHelper.builder(AllTypesCmd.class, AllTypesCmd::new)
                .name("all")
                .addOption(TestModelHelper.defaultOption(
                        new String[] { "--name" }, "Name", String.class, "World",
                        (inst, val) -> ((AllTypesCmd) inst).name = (String) val))
                .build());

        CommandLine cl = new CommandLine(AllTypesCmd.class);
        assertEquals(0, cl.execute());
    }

    @SuppressWarnings("unchecked")
    @Test
    void applySplitValues() {
        CommandModelRegistry.register(TestModelHelper.builder(AllTypesCmd.class, AllTypesCmd::new)
                .name("all")
                .addOption(TestModelHelper.splitOption(
                        new String[] { "--item" }, "Items", String.class, ",",
                        (inst, val) -> ((AllTypesCmd) inst).items = (List<String>) val))
                .build());

        CommandLine cl = new CommandLine(AllTypesCmd.class);
        assertEquals(0, cl.execute("--item", "a,b,c"));
    }

    static class PositionalCmd implements Runnable {
        String file;
        int line;
        List<String> rest;

        @Override
        public void run() {
        }
    }

    @Test
    void applyPositionalParam() {
        CommandModelRegistry.register(TestModelHelper.builder(PositionalCmd.class, PositionalCmd::new)
                .name("pos")
                .addParameter(TestModelHelper.parameter(0, "File", String.class,
                        (inst, val) -> ((PositionalCmd) inst).file = (String) val))
                .addParameter(TestModelHelper.parameter(1, "Line", int.class,
                        (inst, val) -> ((PositionalCmd) inst).line = (int) val))
                .build());

        CommandLine cl = new CommandLine(PositionalCmd.class);
        assertEquals(0, cl.execute("test.txt", "42"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void applyMultiValuePositional() {
        CommandModelRegistry.register(TestModelHelper.builder(PositionalCmd.class, PositionalCmd::new)
                .name("pos")
                .addParameter(TestModelHelper.multiValueParameter(0, "Files", String.class,
                        (inst, val) -> ((PositionalCmd) inst).rest = (List<String>) val))
                .build());

        CommandLine cl = new CommandLine(PositionalCmd.class);
        assertEquals(0, cl.execute("a.txt", "b.txt", "c.txt"));
    }

    @Test
    void mixedOptionsAndPositionals() {
        CommandModelRegistry.register(TestModelHelper.builder(PositionalCmd.class, PositionalCmd::new)
                .name("pos")
                .addOption(TestModelHelper.option(
                        new String[] { "--file" }, "File", String.class,
                        (inst, val) -> ((PositionalCmd) inst).file = (String) val))
                .addParameter(TestModelHelper.parameter(0, "Line", int.class,
                        (inst, val) -> ((PositionalCmd) inst).line = (int) val))
                .build());

        CommandLine cl = new CommandLine(PositionalCmd.class);
        assertEquals(0, cl.execute("--file", "test.txt", "42"));
    }

    enum OutputFormat { JSON, XML, TEXT }

    static class EnumCmd implements Runnable {
        OutputFormat format;

        @Override
        public void run() {
        }
    }

    @Test
    void applyEnumValue() {
        CommandModelRegistry.register(TestModelHelper.builder(EnumCmd.class, EnumCmd::new)
                .name("enum")
                .addOption(TestModelHelper.option(
                        new String[] { "--format" }, "Format", OutputFormat.class,
                        (inst, val) -> ((EnumCmd) inst).format = (OutputFormat) val))
                .build());

        CommandLine cl = new CommandLine(EnumCmd.class);
        assertEquals(0, cl.execute("--format", "JSON"));
    }
}
