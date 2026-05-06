package io.quarkus.quickcli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.Test;

import io.quarkus.quickcli.model.CommandModelRegistry;

class HelpFormatterTest {

    static class HelpCmd implements Runnable {
        @Override
        public void run() {
        }
    }

    private String captureHelp(CommandSpec spec) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        HelpFormatter.printHelp(spec, new PrintStream(baos));
        return baos.toString();
    }

    @Test
    void helpShowsCommandName() {
        CommandModelRegistry.register(TestModelHelper.builder(HelpCmd.class, HelpCmd::new)
                .name("myapp")
                .build());

        CommandLine cmd = new CommandLine(HelpCmd.class);
        String help = captureHelp(cmd.getCommandSpec());
        assertTrue(help.contains("myapp"));
    }

    @Test
    void helpShowsDescription() {
        CommandModelRegistry.register(TestModelHelper.builder(HelpCmd.class, HelpCmd::new)
                .name("myapp")
                .description("A great application")
                .build());

        CommandLine cmd = new CommandLine(HelpCmd.class);
        String help = captureHelp(cmd.getCommandSpec());
        assertTrue(help.contains("A great application"));
    }

    @Test
    void helpShowsOptions() {
        CommandModelRegistry.register(TestModelHelper.builder(HelpCmd.class, HelpCmd::new)
                .name("myapp")
                .addOption(TestModelHelper.option(
                        new String[] { "-n", "--name" }, "Your name", String.class,
                        (inst, val) -> {
                        }))
                .build());

        CommandLine cmd = new CommandLine(HelpCmd.class);
        String help = captureHelp(cmd.getCommandSpec());
        assertTrue(help.contains("-n"));
        assertTrue(help.contains("--name"));
        assertTrue(help.contains("Your name"));
    }

    @Test
    void helpShowsHeader() {
        CommandModelRegistry.register(TestModelHelper.builder(HelpCmd.class, HelpCmd::new)
                .name("myapp")
                .header("My App v1.0")
                .build());

        CommandLine cmd = new CommandLine(HelpCmd.class);
        String help = captureHelp(cmd.getCommandSpec());
        assertTrue(help.contains("My App v1.0"));
    }

    @Test
    void helpShowsFooter() {
        CommandModelRegistry.register(TestModelHelper.builder(HelpCmd.class, HelpCmd::new)
                .name("myapp")
                .footer("Copyright 2024")
                .build());

        CommandLine cmd = new CommandLine(HelpCmd.class);
        String help = captureHelp(cmd.getCommandSpec());
        assertTrue(help.contains("Copyright 2024"));
    }

    @Test
    void helpHidesHiddenOptions() {
        CommandModelRegistry.register(TestModelHelper.builder(HelpCmd.class, HelpCmd::new)
                .name("myapp")
                .addOption(TestModelHelper.option(
                        new String[] { "--visible" }, "Visible option", String.class,
                        (inst, val) -> {
                        }))
                .addOption(TestModelHelper.hiddenOption(
                        new String[] { "--secret" }, "Secret option", String.class,
                        (inst, val) -> {
                        }))
                .build());

        CommandLine cmd = new CommandLine(HelpCmd.class);
        String help = captureHelp(cmd.getCommandSpec());
        assertTrue(help.contains("--visible"));
        assertFalse(help.contains("--secret"));
    }

    @Test
    void helpShowsStandardHelpOptions() {
        CommandModelRegistry.register(TestModelHelper.builder(HelpCmd.class, HelpCmd::new)
                .name("myapp")
                .mixinStandardHelpOptions(true)
                .build());

        CommandLine cmd = new CommandLine(HelpCmd.class);
        String help = captureHelp(cmd.getCommandSpec());
        assertTrue(help.contains("--help"));
        assertTrue(help.contains("--version"));
    }

    @Test
    void helpShowsParameters() {
        CommandModelRegistry.register(TestModelHelper.builder(HelpCmd.class, HelpCmd::new)
                .name("myapp")
                .addParameter(TestModelHelper.parameter(0, "Input file", String.class,
                        (inst, val) -> {
                        }))
                .build());

        CommandLine cmd = new CommandLine(HelpCmd.class);
        String help = captureHelp(cmd.getCommandSpec());
        assertTrue(help.contains("Input file"));
    }

    static class SubHelpCmd implements Runnable {
        @Override
        public void run() {
        }
    }

    @Test
    void helpShowsSubcommands() {
        CommandModelRegistry.register(TestModelHelper.builder(SubHelpCmd.class, SubHelpCmd::new)
                .name("sub")
                .description("A subcommand")
                .build());
        CommandModelRegistry.register(TestModelHelper.builder(HelpCmd.class, HelpCmd::new)
                .name("myapp")
                .addSubcommand(SubHelpCmd.class)
                .build());

        CommandLine cmd = new CommandLine(HelpCmd.class);
        String help = captureHelp(cmd.getCommandSpec());
        assertTrue(help.contains("sub"));
    }
}
