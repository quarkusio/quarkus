package io.quarkus.quickcli;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.quickcli.model.CommandModelRegistry;

class ParseResultTest {

    static class Cmd implements Runnable {
        String name;
        boolean verbose;

        @Override
        public void run() {
        }
    }

    static class SubCmd implements Runnable {
        @Override
        public void run() {
        }
    }

    @Test
    void originalArgs() {
        CommandModelRegistry.register(TestModelHelper.builder(Cmd.class, Cmd::new)
                .name("cmd")
                .addOption(TestModelHelper.option(
                        new String[] { "-n", "--name" }, "Name", String.class,
                        (inst, val) -> ((Cmd) inst).name = (String) val))
                .build());

        CommandLine cmd = new CommandLine(Cmd.class);
        ParseResult result = cmd.parse("-n", "Alice");
        assertEquals(List.of("-n", "Alice"), result.originalArgs());
    }

    @Test
    void matchedOptionByName() {
        CommandModelRegistry.register(TestModelHelper.builder(Cmd.class, Cmd::new)
                .name("cmd")
                .addOption(TestModelHelper.option(
                        new String[] { "-n", "--name" }, "Name", String.class,
                        (inst, val) -> ((Cmd) inst).name = (String) val))
                .build());

        CommandLine cmd = new CommandLine(Cmd.class);
        ParseResult result = cmd.parse("--name", "Bob");
        assertNotNull(result.matchedOption("--name"));
        assertNotNull(result.matchedOption("-n"));
        assertNull(result.matchedOption("--unknown"));
    }

    @Test
    void matchedOptionNotPresent() {
        CommandModelRegistry.register(TestModelHelper.builder(Cmd.class, Cmd::new)
                .name("cmd")
                .addOption(TestModelHelper.option(
                        new String[] { "--name" }, "Name", String.class,
                        (inst, val) -> ((Cmd) inst).name = (String) val))
                .build());

        CommandLine cmd = new CommandLine(Cmd.class);
        ParseResult result = cmd.parse();
        assertNull(result.matchedOption("--name"));
    }

    @Test
    void hasOptionBySpec() {
        CommandModelRegistry.register(TestModelHelper.builder(Cmd.class, Cmd::new)
                .name("cmd")
                .addOption(TestModelHelper.option(
                        new String[] { "-n", "--name" }, "Name", String.class,
                        (inst, val) -> ((Cmd) inst).name = (String) val))
                .build());

        CommandLine cmd = new CommandLine(Cmd.class);
        ParseResult result = cmd.parse("-n", "test");
        OptionSpec opt = cmd.getCommandSpec().options().get(0);
        assertTrue(result.hasOptionBySpec(opt));
    }

    @Test
    void resolveOptionValue() {
        CommandModelRegistry.register(TestModelHelper.builder(Cmd.class, Cmd::new)
                .name("cmd")
                .addOption(TestModelHelper.option(
                        new String[] { "-n", "--name" }, "Name", String.class,
                        (inst, val) -> ((Cmd) inst).name = (String) val))
                .build());

        CommandLine cmd = new CommandLine(Cmd.class);
        ParseResult result = cmd.parse("-n", "resolved");
        OptionSpec opt = cmd.getCommandSpec().options().get(0);
        assertEquals("resolved", result.resolveOptionValue(opt));
    }

    @Test
    void subcommandAlias() {
        CommandModelRegistry.register(TestModelHelper.builder(SubCmd.class, SubCmd::new)
                .name("sub").build());
        CommandModelRegistry.register(TestModelHelper.builder(Cmd.class, Cmd::new)
                .name("cmd")
                .addSubcommand(SubCmd.class)
                .build());

        CommandLine cmd = new CommandLine(Cmd.class);
        ParseResult result = cmd.parse("sub");
        // subcommand() is an alias for subcommandResult()
        assertSame(result.subcommandResult(), result.subcommand());
    }

    @Test
    void getOptionValuesEmpty() {
        CommandModelRegistry.register(TestModelHelper.builder(Cmd.class, Cmd::new)
                .name("cmd").build());

        CommandLine cmd = new CommandLine(Cmd.class);
        ParseResult result = cmd.parse();
        assertTrue(result.getOptionValues("--nonexistent").isEmpty());
    }

    @Test
    void getOptionValueNull() {
        CommandModelRegistry.register(TestModelHelper.builder(Cmd.class, Cmd::new)
                .name("cmd").build());

        CommandLine cmd = new CommandLine(Cmd.class);
        ParseResult result = cmd.parse();
        assertNull(result.getOptionValue("--nonexistent"));
    }

    @Test
    void getPositionalValueOutOfBounds() {
        CommandModelRegistry.register(TestModelHelper.builder(Cmd.class, Cmd::new)
                .name("cmd").build());

        CommandLine cmd = new CommandLine(Cmd.class);
        ParseResult result = cmd.parse();
        assertNull(result.getPositionalValue(0));
    }

    @Test
    void multipleValuesForSameOption() {
        CommandModelRegistry.register(TestModelHelper.builder(Cmd.class, Cmd::new)
                .name("cmd")
                .addOption(TestModelHelper.listOption(
                        new String[] { "--item" }, "Items", String.class,
                        (inst, val) -> {
                        }))
                .build());

        CommandLine cmd = new CommandLine(Cmd.class);
        ParseResult result = cmd.parse("--item", "a", "--item", "b");
        assertEquals(List.of("a", "b"), result.getOptionValues("--item"));
        // getOptionValue returns the first
        assertEquals("a", result.getOptionValue("--item"));
    }
}
