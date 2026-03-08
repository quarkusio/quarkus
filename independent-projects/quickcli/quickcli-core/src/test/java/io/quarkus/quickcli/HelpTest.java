package io.quarkus.quickcli;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.quarkus.quickcli.model.CommandModelRegistry;

class HelpTest {

    static class HelpCmd implements Runnable {
        @Override
        public void run() {
        }
    }

    static class SubHelpCmd implements Runnable {
        @Override
        public void run() {
        }
    }

    @Test
    void fullSynopsisContainsName() {
        CommandModelRegistry.register(TestModelHelper.builder(HelpCmd.class, HelpCmd::new)
                .name("myapp").build());

        CommandLine cmd = new CommandLine(HelpCmd.class);
        Help help = cmd.getHelp();
        String synopsis = help.fullSynopsis();
        assertTrue(synopsis.contains("myapp"));
    }

    @Test
    void fullSynopsisShowsOptions() {
        CommandModelRegistry.register(TestModelHelper.builder(HelpCmd.class, HelpCmd::new)
                .name("myapp")
                .addOption(TestModelHelper.option(
                        new String[] { "--name" }, "Name", String.class,
                        (inst, val) -> {
                        }))
                .build());

        CommandLine cmd = new CommandLine(HelpCmd.class);
        assertTrue(cmd.getHelp().fullSynopsis().contains("[OPTIONS]"));
    }

    @Test
    void fullSynopsisShowsCommand() {
        CommandModelRegistry.register(TestModelHelper.builder(SubHelpCmd.class, SubHelpCmd::new)
                .name("sub").build());
        CommandModelRegistry.register(TestModelHelper.builder(HelpCmd.class, HelpCmd::new)
                .name("myapp")
                .addSubcommand(SubHelpCmd.class)
                .build());

        CommandLine cmd = new CommandLine(HelpCmd.class);
        assertTrue(cmd.getHelp().fullSynopsis().contains("[COMMAND]"));
    }

    @Test
    void fullSynopsisShowsParameters() {
        CommandModelRegistry.register(TestModelHelper.builder(HelpCmd.class, HelpCmd::new)
                .name("myapp")
                .addParameter(TestModelHelper.parameter(0, "Input file", String.class,
                        (inst, val) -> {
                        }))
                .build());

        CommandLine cmd = new CommandLine(HelpCmd.class);
        String synopsis = cmd.getHelp().fullSynopsis();
        assertTrue(synopsis.contains("<param0>") || synopsis.contains("param0"));
    }

    @Test
    void commandListEmpty() {
        CommandModelRegistry.register(TestModelHelper.builder(HelpCmd.class, HelpCmd::new)
                .name("myapp").build());

        CommandLine cmd = new CommandLine(HelpCmd.class);
        assertEquals("", cmd.getHelp().commandList());
    }

    @Test
    void commandListWithSubcommands() {
        CommandModelRegistry.register(TestModelHelper.builder(SubHelpCmd.class, SubHelpCmd::new)
                .name("sub")
                .description("A subcommand")
                .build());
        CommandModelRegistry.register(TestModelHelper.builder(HelpCmd.class, HelpCmd::new)
                .name("myapp")
                .addSubcommand(SubHelpCmd.class)
                .build());

        CommandLine cmd = new CommandLine(HelpCmd.class);
        String list = cmd.getHelp().commandList();
        assertTrue(list.contains("sub"));
        assertTrue(list.contains("A subcommand"));
    }

    @Test
    void formatHeadingReplacesNewlines() {
        assertEquals("Title" + System.lineSeparator(), Help.formatHeading("Title%n"));
    }

    @Test
    void formatHeadingEmpty() {
        assertEquals("", Help.formatHeading(""));
        assertEquals("", Help.formatHeading(null));
    }

    @Test
    void colorSchemeErrorText() {
        Help.ColorScheme cs = Help.ColorScheme.DEFAULT;
        assertEquals("Error occurred", cs.errorText("@|red Error occurred|@"));
    }

    @Test
    void colorSchemeStackTraceText() {
        Help.ColorScheme cs = Help.ColorScheme.DEFAULT;
        String trace = cs.stackTraceText(new RuntimeException("test"));
        assertTrue(trace.contains("RuntimeException"));
        assertTrue(trace.contains("test"));
    }

    @Test
    void textTableRendering() {
        Help.TextTable table = Help.TextTable.forColumns(
                Help.ColorScheme.DEFAULT,
                new Help.Column(20, 2, Help.Column.Overflow.SPAN),
                new Help.Column(40, 2, Help.Column.Overflow.WRAP));
        table.addRowValues("--name", "Your name");
        table.addRowValues("--count", "Number of times");
        String output = table.toString();
        assertTrue(output.contains("--name"));
        assertTrue(output.contains("Your name"));
        assertTrue(output.contains("--count"));
    }

    @Test
    void helpCommandSpec() {
        CommandModelRegistry.register(TestModelHelper.builder(HelpCmd.class, HelpCmd::new)
                .name("myapp").build());

        CommandLine cmd = new CommandLine(HelpCmd.class);
        Help help = new Help(cmd.getCommandSpec());
        assertSame(cmd.getCommandSpec(), help.commandSpec());
    }

    @Test
    void helpColorScheme() {
        CommandModelRegistry.register(TestModelHelper.builder(HelpCmd.class, HelpCmd::new)
                .name("myapp").build());

        CommandLine cmd = new CommandLine(HelpCmd.class);
        Help help = new Help(cmd.getCommandSpec(), Help.ColorScheme.DEFAULT);
        assertSame(Help.ColorScheme.DEFAULT, help.colorScheme());
    }

    @Test
    void colorSchemeAnsi() {
        assertEquals(Ansi.AUTO, Help.ColorScheme.DEFAULT.ansi());
    }
}
