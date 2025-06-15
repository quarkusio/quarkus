package io.quarkus.cli.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import picocli.CommandLine;

public class PluginCommandFactoryTest {

    final PluginCommandFactory factory = new PluginCommandFactory();

    @Test
    void testNoArgs() {
        // given
        TestCommand rootCommand = new TestCommand();
        CommandLine commandLine = new CommandLine(factory.createCommandSpec("rootCommand command").apply(rootCommand));
        // when
        commandLine.execute(new String[0]);
        // thew
        assertEquals(0, rootCommand.getArguments().size());
    }

    @Test
    void testSingleArg() {
        // given
        TestCommand rootCommand = new TestCommand();
        CommandLine commandLine = new CommandLine(factory.createCommandSpec("rootCommand command").apply(rootCommand));
        // when
        commandLine.execute("hello");
        // then
        assertEquals("hello", rootCommand.getArguments().get(0));
    }

    @Test
    void testMultiArgs() {
        // given
        TestCommand rootCommand = new TestCommand();
        CommandLine commandLine = new CommandLine(factory.createCommandSpec("rootCommand command").apply(rootCommand));
        // when
        commandLine.execute("one", "two", "three");
        // then
        assertEquals("one", rootCommand.getArguments().get(0));
        assertEquals("two", rootCommand.getArguments().get(1));
        assertEquals("three", rootCommand.getArguments().get(2));
    }

    @Test
    void testMultiArgsAndOptions() {
        // given
        TestCommand rootCommand = new TestCommand();
        CommandLine commandLine = new CommandLine(factory.createCommandSpec("rootCommand command").apply(rootCommand));
        // when
        commandLine.execute("one", "two", "three", "--depth", "5");
        // then
        assertEquals("one", rootCommand.getArguments().get(0));
        assertEquals("two", rootCommand.getArguments().get(1));
        assertEquals("three", rootCommand.getArguments().get(2));
        assertEquals("--depth", rootCommand.getArguments().get(3));
        assertEquals("5", rootCommand.getArguments().get(4));
    }

    @Test
    void testSubCommandNoArgs() {
        // given
        TestCommand rootCommand = new TestCommand();
        TestCommand subCommand = new TestCommand();
        CommandLine commandLine = new CommandLine(factory.createCommandSpec("rootCommand command").apply(rootCommand))
                .addSubcommand("sub", new CommandLine(factory.createCommandSpec("sub command").apply(subCommand)));

        // when
        commandLine.execute(new String[0]);
        // thew
        assertEquals(0, rootCommand.getArguments().size());
        assertEquals(0, subCommand.getArguments().size());
    }

    @Test
    void testSubSingleArg() {
        // given
        TestCommand rootCommand = new TestCommand();
        TestCommand subCommand = new TestCommand();
        CommandLine commandLine = new CommandLine(factory.createCommandSpec("rootCommand command").apply(rootCommand))
                .addSubcommand("sub", new CommandLine(factory.createCommandSpec("sub command").apply(subCommand)));

        // when
        commandLine.execute("sub", "hello");
        // then
        assertEquals("hello", subCommand.getArguments().get(0));
    }

    @Test
    void testSubMultiArgs() {
        // given
        TestCommand rootCommand = new TestCommand();
        TestCommand subCommand = new TestCommand();
        CommandLine commandLine = new CommandLine(factory.createCommandSpec("rootCommand command").apply(rootCommand))
                .addSubcommand("sub", new CommandLine(factory.createCommandSpec("sub command").apply(subCommand)));
        // when
        commandLine.execute("sub", "one", "two", "three");
        // then
        assertEquals("one", subCommand.getArguments().get(0));
        assertEquals("two", subCommand.getArguments().get(1));
        assertEquals("three", subCommand.getArguments().get(2));
    }

    @Test
    void testSubMultiArgsAndOptions() {
        // given
        TestCommand rootCommand = new TestCommand();
        TestCommand subCommand = new TestCommand();
        CommandLine commandLine = new CommandLine(factory.createCommandSpec("rootCommand command").apply(rootCommand))
                .addSubcommand("sub", new CommandLine(factory.createCommandSpec("sub command").apply(subCommand)));
        // when
        commandLine.execute("sub", "one", "two", "three", "--depth", "5");
        // then
        assertEquals("one", subCommand.getArguments().get(0));
        assertEquals("two", subCommand.getArguments().get(1));
        assertEquals("three", subCommand.getArguments().get(2));
        assertEquals("--depth", subCommand.getArguments().get(3));
        assertEquals("5", subCommand.getArguments().get(4));
    }

    @Test
    void testSecLevelSubCommandNoArgs() {
        // given
        TestCommand rootCommand = new TestCommand();
        TestCommand subCommand = new TestCommand();
        TestCommand secondLevelSubCommand = new TestCommand();

        CommandLine commandLine = new CommandLine(factory.createCommandSpec("rootCommand command").apply(rootCommand))
                .addSubcommand("sub", new CommandLine(factory.createCommandSpec("sub command").apply(subCommand))
                        .addSubcommand("sec-sub", new CommandLine(
                                factory.createCommandSpec("secon level sub command").apply(secondLevelSubCommand))));

        // when
        commandLine.execute(new String[0]);
        // thew
        assertEquals(0, rootCommand.getArguments().size());
        assertEquals(0, subCommand.getArguments().size());
        assertEquals(0, secondLevelSubCommand.getArguments().size());
    }

    @Test
    void testSecLevelSubSingleArg() {
        // given
        TestCommand rootCommand = new TestCommand();
        TestCommand subCommand = new TestCommand();
        TestCommand secondLevelSubCommand = new TestCommand();
        CommandLine commandLine = new CommandLine(factory.createCommandSpec("rootCommand command").apply(rootCommand))
                .addSubcommand("sub", new CommandLine(factory.createCommandSpec("sub command").apply(subCommand))
                        .addSubcommand("sec-sub", new CommandLine(
                                factory.createCommandSpec("secon level sub command").apply(secondLevelSubCommand))));

        // when
        commandLine.execute("sub", "sec-sub", "hello");
        // then
        assertEquals("hello", secondLevelSubCommand.getArguments().get(0));
    }

    @Test
    void testSecLevelSubMultiArgs() {
        // given
        TestCommand rootCommand = new TestCommand();
        TestCommand subCommand = new TestCommand();
        TestCommand secondLevelSubCommand = new TestCommand();
        CommandLine commandLine = new CommandLine(factory.createCommandSpec("rootCommand command").apply(rootCommand))
                .addSubcommand("sub", new CommandLine(factory.createCommandSpec("sub command").apply(subCommand))
                        .addSubcommand("sec-sub", new CommandLine(
                                factory.createCommandSpec("secon level sub command").apply(secondLevelSubCommand))));
        // when
        commandLine.execute("sub", "sec-sub", "one", "two", "three");
        // then
        assertEquals("one", secondLevelSubCommand.getArguments().get(0));
        assertEquals("two", secondLevelSubCommand.getArguments().get(1));
        assertEquals("three", secondLevelSubCommand.getArguments().get(2));
    }

    @Test
    void testSecLevelSubMultiArgsAndOptions() {
        // given
        TestCommand rootCommand = new TestCommand();
        TestCommand subCommand = new TestCommand();
        TestCommand secondLevelSubCommand = new TestCommand();
        CommandLine commandLine = new CommandLine(factory.createCommandSpec("rootCommand command").apply(rootCommand))
                .addSubcommand("sub", new CommandLine(factory.createCommandSpec("sub command").apply(subCommand))
                        .addSubcommand("sec-sub", new CommandLine(
                                factory.createCommandSpec("secon level sub command").apply(secondLevelSubCommand))));
        // when
        commandLine.execute("sub", "sec-sub", "one", "two", "three", "--depth", "5");
        // then
        assertEquals("one", secondLevelSubCommand.getArguments().get(0));
        assertEquals("two", secondLevelSubCommand.getArguments().get(1));
        assertEquals("three", secondLevelSubCommand.getArguments().get(2));
        assertEquals("--depth", secondLevelSubCommand.getArguments().get(3));
        assertEquals("5", secondLevelSubCommand.getArguments().get(4));
    }
}
