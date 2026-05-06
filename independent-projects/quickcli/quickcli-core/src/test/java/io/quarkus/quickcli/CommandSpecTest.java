package io.quarkus.quickcli;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.quickcli.model.CommandModelRegistry;

class CommandSpecTest {

    static class SpecCmd implements Runnable {
        @Override
        public void run() {
        }
    }

    static class SubSpecCmd implements Runnable {
        @Override
        public void run() {
        }
    }

    @Test
    void specHasName() {
        CommandModelRegistry.register(TestModelHelper.builder(SpecCmd.class, SpecCmd::new)
                .name("myapp")
                .build());

        CommandLine cmd = new CommandLine(SpecCmd.class);
        assertEquals("myapp", cmd.getCommandSpec().name());
    }

    @Test
    void specHasDescription() {
        CommandModelRegistry.register(TestModelHelper.builder(SpecCmd.class, SpecCmd::new)
                .name("myapp")
                .description("Line 1", "Line 2")
                .build());

        CommandLine cmd = new CommandLine(SpecCmd.class);
        assertArrayEquals(new String[] { "Line 1", "Line 2" },
                cmd.getCommandSpec().description());
    }

    @Test
    void specHasVersion() {
        CommandModelRegistry.register(TestModelHelper.builder(SpecCmd.class, SpecCmd::new)
                .name("myapp")
                .version("1.0.0", "Build 123")
                .build());

        CommandLine cmd = new CommandLine(SpecCmd.class);
        assertArrayEquals(new String[] { "1.0.0", "Build 123" },
                cmd.getCommandSpec().version());
    }

    @Test
    void specOptions() {
        CommandModelRegistry.register(TestModelHelper.builder(SpecCmd.class, SpecCmd::new)
                .name("myapp")
                .addOption(TestModelHelper.option(
                        new String[] { "-n", "--name" }, "Name", String.class,
                        (inst, val) -> {
                        }))
                .build());

        CommandLine cmd = new CommandLine(SpecCmd.class);
        assertEquals(1, cmd.getCommandSpec().options().size());
        OptionSpec opt = cmd.getCommandSpec().options().get(0);
        assertArrayEquals(new String[] { "-n", "--name" }, opt.names());
        assertEquals("--name", opt.longestName());
        assertEquals("Name", opt.description());
    }

    @Test
    void specParameters() {
        CommandModelRegistry.register(TestModelHelper.builder(SpecCmd.class, SpecCmd::new)
                .name("myapp")
                .addParameter(TestModelHelper.parameter(0, "Input file", String.class,
                        (inst, val) -> {
                        }))
                .build());

        CommandLine cmd = new CommandLine(SpecCmd.class);
        assertEquals(1, cmd.getCommandSpec().parameters().size());
        ParameterSpec param = cmd.getCommandSpec().parameters().get(0);
        assertEquals(0, param.index());
        assertEquals("Input file", param.description());
    }

    @Test
    void specFindOption() {
        CommandModelRegistry.register(TestModelHelper.builder(SpecCmd.class, SpecCmd::new)
                .name("myapp")
                .addOption(TestModelHelper.option(
                        new String[] { "-n", "--name" }, "Name", String.class,
                        (inst, val) -> {
                        }))
                .build());

        CommandLine cmd = new CommandLine(SpecCmd.class);
        CommandSpec spec = cmd.getCommandSpec();
        assertNotNull(spec.findOption("-n"));
        assertNotNull(spec.findOption("--name"));
        assertNull(spec.findOption("--unknown"));
    }

    @Test
    void specSubcommands() {
        CommandModelRegistry.register(TestModelHelper.builder(SubSpecCmd.class, SubSpecCmd::new)
                .name("sub")
                .description("A subcommand")
                .build());
        CommandModelRegistry.register(TestModelHelper.builder(SpecCmd.class, SpecCmd::new)
                .name("myapp")
                .addSubcommand(SubSpecCmd.class)
                .build());

        CommandLine cmd = new CommandLine(SpecCmd.class);
        assertTrue(cmd.getSubcommands().containsKey("sub"));
        assertEquals("sub", cmd.getCommandSpec().subcommands().get("sub").name());
    }

    @Test
    void specAliases() {
        CommandModelRegistry.register(TestModelHelper.builder(SpecCmd.class, SpecCmd::new)
                .name("myapp")
                .aliases("app", "ma")
                .build());

        CommandLine cmd = new CommandLine(SpecCmd.class);
        assertArrayEquals(new String[] { "app", "ma" }, cmd.getCommandSpec().aliases());
    }

    @Test
    void specQualifiedName() {
        CommandModelRegistry.register(TestModelHelper.builder(SubSpecCmd.class, SubSpecCmd::new)
                .name("sub")
                .build());
        CommandModelRegistry.register(TestModelHelper.builder(SpecCmd.class, SpecCmd::new)
                .name("myapp")
                .addSubcommand(SubSpecCmd.class)
                .build());

        CommandLine cmd = new CommandLine(SpecCmd.class);
        CommandSpec subSpec = cmd.getCommandSpec().subcommands().get("sub");
        assertEquals("myapp sub", subSpec.qualifiedName());
    }

    @Test
    void specAddSubcommandAtRuntime() {
        CommandModelRegistry.register(TestModelHelper.builder(SpecCmd.class, SpecCmd::new)
                .name("myapp")
                .build());
        CommandModelRegistry.register(TestModelHelper.builder(SubSpecCmd.class, SubSpecCmd::new)
                .name("dynamic")
                .build());

        CommandLine cmd = new CommandLine(SpecCmd.class);
        cmd.addSubcommand("dynamic", new CommandLine(SubSpecCmd.class));
        assertTrue(cmd.getSubcommands().containsKey("dynamic"));
    }

    @Test
    void specCreateForPlugins() {
        CommandSpec spec = CommandSpec.create("plugin", Runnable.class);
        assertEquals("plugin", spec.name());
        assertEquals(Runnable.class, spec.commandClass());
    }

    @Test
    void specHeaderAndFooter() {
        CommandModelRegistry.register(TestModelHelper.builder(SpecCmd.class, SpecCmd::new)
                .name("myapp")
                .header("Header line")
                .footer("Footer line")
                .build());

        CommandLine cmd = new CommandLine(SpecCmd.class);
        assertArrayEquals(new String[] { "Header line" }, cmd.getCommandSpec().header());
        assertArrayEquals(new String[] { "Footer line" }, cmd.getCommandSpec().footer());
    }

    @Test
    void specExclusiveGroups() {
        CommandModelRegistry.register(TestModelHelper.builder(SpecCmd.class, SpecCmd::new)
                .name("myapp")
                .addOption(TestModelHelper.booleanOption(
                        new String[] { "--json" }, "JSON",
                        (inst, val) -> {
                        }))
                .addOption(TestModelHelper.booleanOption(
                        new String[] { "--xml" }, "XML",
                        (inst, val) -> {
                        }))
                .addExclusiveGroup(List.of("--json", "--xml"))
                .build());

        CommandLine cmd = new CommandLine(SpecCmd.class);
        assertEquals(1, cmd.getCommandSpec().exclusiveGroups().size());
        assertEquals(List.of("--json", "--xml"), cmd.getCommandSpec().exclusiveGroups().get(0));
    }

    @Test
    void optionSpecMatches() {
        OptionSpec opt = new OptionSpec(
                new String[] { "-n", "--name" }, "Name", String.class,
                false, "", "", false, "", "name", false, false, false, -1);
        assertTrue(opt.matches("-n"));
        assertTrue(opt.matches("--name"));
        assertFalse(opt.matches("--unknown"));
    }

    @Test
    void optionSpecIsBoolean() {
        OptionSpec boolOpt = new OptionSpec(
                new String[] { "-v" }, "Verbose", boolean.class,
                false, "", "", false, "", "verbose", false, false, false, -1);
        assertTrue(boolOpt.isBoolean());

        OptionSpec strOpt = new OptionSpec(
                new String[] { "-n" }, "Name", String.class,
                false, "", "", false, "", "name", false, false, false, -1);
        assertFalse(strOpt.isBoolean());
    }
}
