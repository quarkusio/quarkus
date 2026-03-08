package io.quarkus.quickcli;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.quickcli.model.BuiltCommandModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class AutoCompleteTest {

    static class TopCmd implements Runnable {
        boolean verbose;

        public void run() {
        }
    }

    static class SubCmd implements Runnable {
        String file;

        public void run() {
        }
    }

    static class LeafCmd implements Runnable {
        boolean force;
        String output;

        public void run() {
        }
    }

    @BeforeAll
    static void setup() {
        // Register TopCmd with a --verbose flag and a "sub" subcommand
        BuiltCommandModel topModel = TestModelHelper.builder(TopCmd.class, TopCmd::new)
                .name("mycli")
                .description("My CLI tool")
                .addOption(TestModelHelper.booleanOption(
                        new String[] { "-v", "--verbose" }, "Verbose output",
                        (obj, val) -> ((TopCmd) obj).verbose = (boolean) val))
                .addSubcommand(SubCmd.class)
                .build();
        TestModelHelper.register(topModel);

        // Register SubCmd with a --file arg option and a "leaf" subcommand
        BuiltCommandModel subModel = TestModelHelper.builder(SubCmd.class, SubCmd::new)
                .name("sub")
                .description("A sub command")
                .addOption(TestModelHelper.option(
                        new String[] { "-f", "--file" }, "Input file", String.class,
                        (obj, val) -> ((SubCmd) obj).file = (String) val))
                .addSubcommand(LeafCmd.class)
                .build();
        TestModelHelper.register(subModel);

        // Register LeafCmd with --force flag and --output arg
        BuiltCommandModel leafModel = TestModelHelper.builder(LeafCmd.class, LeafCmd::new)
                .name("leaf")
                .description("A leaf command")
                .addOption(TestModelHelper.booleanOption(
                        new String[] { "--force" }, "Force operation",
                        (obj, val) -> ((LeafCmd) obj).force = (boolean) val))
                .addOption(TestModelHelper.option(
                        new String[] { "-o", "--output" }, "Output path", String.class,
                        (obj, val) -> ((LeafCmd) obj).output = (String) val))
                .build();
        TestModelHelper.register(leafModel);
    }

    @Test
    void generatesValidBashScript() {
        CommandSpec rootSpec = new CommandLine(TopCmd.class).getCommandSpec();
        String script = AutoComplete.bash("mycli", rootSpec);

        // Header
        assertTrue(script.startsWith("#!/usr/bin/env bash"), "Should start with shebang");
        assertTrue(script.contains("mycli Bash Completion"));

        // Entry point function
        assertTrue(script.contains("function _complete_mycli()"));

        // Footer: complete registration
        assertTrue(script.contains("complete -F _complete_mycli -o default mycli"));
    }

    @Test
    void containsSubcommandNames() {
        CommandSpec rootSpec = new CommandLine(TopCmd.class).getCommandSpec();
        String script = AutoComplete.bash("mycli", rootSpec);

        // The top-level function should list "sub" as a command
        assertTrue(script.contains("local commands=\"sub\""),
                "Top-level function should list subcommand 'sub'");
    }

    @Test
    void containsFlagOptions() {
        CommandSpec rootSpec = new CommandLine(TopCmd.class).getCommandSpec();
        String script = AutoComplete.bash("mycli", rootSpec);

        // The top-level function should list --verbose as a flag
        assertTrue(script.contains("'-v'") && script.contains("'--verbose'"),
                "Should contain verbose flag options");
    }

    @Test
    void containsArgOptions() {
        CommandSpec rootSpec = new CommandLine(TopCmd.class).getCommandSpec();
        String script = AutoComplete.bash("mycli", rootSpec);

        // The sub function should have --file as an arg option
        assertTrue(script.contains("'-f'") && script.contains("'--file'"),
                "Should contain file arg options");
    }

    @Test
    void generatesNestedSubcommandFunctions() {
        CommandSpec rootSpec = new CommandLine(TopCmd.class).getCommandSpec();
        String script = AutoComplete.bash("mycli", rootSpec);

        // Should generate functions for sub and leaf
        assertTrue(script.contains("function _picocli_mycli_sub()"),
                "Should generate function for 'sub' subcommand");
        assertTrue(script.contains("function _picocli_mycli_sub_leaf()"),
                "Should generate function for 'leaf' subcommand");
    }

    @Test
    void leafCommandHasFlagAndArgOptions() {
        CommandSpec rootSpec = new CommandLine(TopCmd.class).getCommandSpec();
        String script = AutoComplete.bash("mycli", rootSpec);

        // leaf should have --force as flag, --output as arg
        assertTrue(script.contains("'--force'"), "leaf should have --force flag");
        assertTrue(script.contains("'--output'"), "leaf should have --output arg");
    }

    @Test
    void edgeCaseLinesGenerated() {
        CommandSpec rootSpec = new CommandLine(TopCmd.class).getCommandSpec();
        String script = AutoComplete.bash("mycli", rootSpec);

        // Edge case: no trailing space after subcommand
        assertTrue(script.contains("\"${COMP_WORDS[0]} sub\""),
                "Should have edge case for 'sub'");
        assertTrue(script.contains("\"${COMP_WORDS[0]} sub leaf\""),
                "Should have edge case for 'sub leaf'");
    }

    @Test
    void compWordsContainsArrayCalls() {
        CommandSpec rootSpec = new CommandLine(TopCmd.class).getCommandSpec();
        String script = AutoComplete.bash("mycli", rootSpec);

        assertTrue(script.contains("CompWordsContainsArray"),
                "Should use CompWordsContainsArray for subcommand matching");
    }

    @Test
    void bashAndZshSupport() {
        CommandSpec rootSpec = new CommandLine(TopCmd.class).getCommandSpec();
        String script = AutoComplete.bash("mycli", rootSpec);

        assertTrue(script.contains("BASH_VERSION"), "Should support bash");
        assertTrue(script.contains("ZSH_VERSION"), "Should support zsh");
        assertTrue(script.contains("bashcompinit"), "Should enable bash compat in zsh");
    }

    @Test
    void sanitizesScriptName() {
        CommandSpec rootSpec = new CommandLine(TopCmd.class).getCommandSpec();
        String script = AutoComplete.bash("./mycli.sh", rootSpec);

        // Should strip .sh and ./
        assertTrue(script.contains("function _complete_mycli()"),
                "Should sanitize script name");
        assertFalse(script.contains("_complete_./mycli"),
                "Should not contain unsanitized name");
    }
}
