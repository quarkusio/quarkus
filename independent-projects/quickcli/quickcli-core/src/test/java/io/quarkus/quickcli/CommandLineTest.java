package io.quarkus.quickcli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;

import io.quarkus.quickcli.model.CommandModelRegistry;

class CommandLineTest {

    static class SimpleCmd implements Runnable {
        String name;
        boolean ran;

        @Override
        public void run() {
            ran = true;
        }
    }

    static class SubCmd implements Runnable {
        @Override
        public void run() {
        }
    }

    static class FailCmd implements Runnable {
        @Override
        public void run() {
            throw new RuntimeException("boom");
        }
    }

    static class CallableCmd implements Callable<Integer> {
        @Override
        public Integer call() {
            return 0;
        }
    }

    static class VersionProviderCmd implements Runnable {
        @Override
        public void run() {
        }
    }

    static class TestVersionProvider implements VersionProvider {
        @Override
        public String[] getVersion() {
            return new String[] { "TestApp 3.0.0" };
        }
    }

    // --- Output/Error streams ---

    @Test
    void setOutAndGetOut() {
        CommandModelRegistry.register(TestModelHelper.builder(SimpleCmd.class, SimpleCmd::new)
                .name("cmd").build());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CommandLine cmd = new CommandLine(SimpleCmd.class);
        cmd.setOut(new PrintStream(baos));
        PrintWriter out = cmd.getOut();
        out.print("hello");
        out.flush();
        assertEquals("hello", baos.toString());
    }

    @Test
    void setErrAndGetErr() {
        CommandModelRegistry.register(TestModelHelper.builder(SimpleCmd.class, SimpleCmd::new)
                .name("cmd").build());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CommandLine cmd = new CommandLine(SimpleCmd.class);
        cmd.setErr(new PrintStream(baos));
        PrintWriter err = cmd.getErr();
        err.print("error");
        err.flush();
        assertEquals("error", baos.toString());
    }

    // --- Command name ---

    @Test
    void getCommandName() {
        CommandModelRegistry.register(TestModelHelper.builder(SimpleCmd.class, SimpleCmd::new)
                .name("myapp").build());

        CommandLine cmd = new CommandLine(SimpleCmd.class);
        assertEquals("myapp", cmd.getCommandName());
    }

    // --- parseArgs alias ---

    @Test
    void parseArgsAlias() {
        CommandModelRegistry.register(TestModelHelper.builder(SimpleCmd.class, SimpleCmd::new)
                .name("cmd")
                .addOption(TestModelHelper.option(
                        new String[] { "--name" }, "Name", String.class,
                        (inst, val) -> ((SimpleCmd) inst).name = (String) val))
                .build());

        CommandLine cmd = new CommandLine(SimpleCmd.class);
        ParseResult result = cmd.parseArgs("--name", "test");
        assertEquals("test", result.getOptionValue("--name"));
    }

    // --- getParseResult ---

    @Test
    void getParseResultAfterParse() {
        CommandModelRegistry.register(TestModelHelper.builder(SimpleCmd.class, SimpleCmd::new)
                .name("cmd").build());

        CommandLine cmd = new CommandLine(SimpleCmd.class);
        ParseResult result = cmd.parse();
        assertSame(result, cmd.getParseResult());
    }

    @Test
    void getParseResultNull() {
        CommandModelRegistry.register(TestModelHelper.builder(SimpleCmd.class, SimpleCmd::new)
                .name("cmd").build());

        CommandLine cmd = new CommandLine(SimpleCmd.class);
        assertNull(cmd.getParseResult());
    }

    // --- getUnmatchedArguments ---

    @Test
    void getUnmatchedArgumentsEmpty() {
        CommandModelRegistry.register(TestModelHelper.builder(SimpleCmd.class, SimpleCmd::new)
                .name("cmd").build());

        CommandLine cmd = new CommandLine(SimpleCmd.class);
        assertTrue(cmd.getUnmatchedArguments().isEmpty());
    }

    @Test
    void getUnmatchedArgumentsWithUnmatched() {
        CommandModelRegistry.register(TestModelHelper.builder(SimpleCmd.class, SimpleCmd::new)
                .name("cmd")
                .hasUnmatchedField(true)
                .unmatchedAccessor((inst, val) -> {
                })
                .build());

        CommandLine cmd = new CommandLine(SimpleCmd.class);
        cmd.parse("--extra", "arg");
        assertEquals(List.of("--extra", "arg"), cmd.getUnmatchedArguments());
    }

    // --- usage ---

    @Test
    void usagePrintsHelp() {
        CommandModelRegistry.register(TestModelHelper.builder(SimpleCmd.class, SimpleCmd::new)
                .name("myapp")
                .description("App desc")
                .build());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CommandLine cmd = new CommandLine(SimpleCmd.class);
        cmd.usage(new PrintStream(baos));
        String output = baos.toString();
        assertTrue(output.contains("myapp"));
        assertTrue(output.contains("App desc"));
    }

    // --- getColorScheme ---

    @Test
    void getColorScheme() {
        CommandModelRegistry.register(TestModelHelper.builder(SimpleCmd.class, SimpleCmd::new)
                .name("cmd").build());

        CommandLine cmd = new CommandLine(SimpleCmd.class);
        assertNotNull(cmd.getColorScheme());
    }

    // --- getHelp ---

    @Test
    void getHelp() {
        CommandModelRegistry.register(TestModelHelper.builder(SimpleCmd.class, SimpleCmd::new)
                .name("cmd").build());

        CommandLine cmd = new CommandLine(SimpleCmd.class);
        Help help = cmd.getHelp();
        assertNotNull(help);
        assertSame(cmd.getCommandSpec(), help.commandSpec());
    }

    // --- Custom execution strategy ---

    @Test
    void customExecutionStrategy() {
        CommandModelRegistry.register(TestModelHelper.builder(SimpleCmd.class, SimpleCmd::new)
                .name("cmd").build());

        CommandLine cmd = new CommandLine(SimpleCmd.class);
        cmd.setExecutionStrategy((parseResult, commandLine) -> 77);
        assertEquals(77, cmd.execute());
    }

    // --- Exit code exception mapper ---

    @Test
    void exitCodeExceptionMapper() {
        CommandModelRegistry.register(TestModelHelper.builder(SimpleCmd.class, SimpleCmd::new)
                .name("cmd").build());

        CommandLine cmd = new CommandLine(SimpleCmd.class);
        assertNull(cmd.getExitCodeExceptionMapper());
        CommandLine.ExitCodeExceptionMapper mapper = ex -> 55;
        cmd.setExitCodeExceptionMapper(mapper);
        assertSame(mapper, cmd.getExitCodeExceptionMapper());
    }

    // --- Help section map ---

    @Test
    void helpSectionMap() {
        CommandModelRegistry.register(TestModelHelper.builder(SimpleCmd.class, SimpleCmd::new)
                .name("cmd").build());

        CommandLine cmd = new CommandLine(SimpleCmd.class);
        assertNotNull(cmd.getHelpSectionMap());
    }

    // --- Subcommand operations ---

    @Test
    void addSubcommandFromCommandLine() {
        CommandModelRegistry.register(TestModelHelper.builder(SimpleCmd.class, SimpleCmd::new)
                .name("myapp").build());
        CommandModelRegistry.register(TestModelHelper.builder(SubCmd.class, SubCmd::new)
                .name("sub").build());

        CommandLine cmd = new CommandLine(SimpleCmd.class);
        CommandLine subCmdLine = new CommandLine(SubCmd.class);
        cmd.addSubcommand("dynamic", subCmdLine);
        assertTrue(cmd.getSubcommands().containsKey("dynamic"));
    }

    @Test
    void getSubcommandsUnmodifiable() {
        CommandModelRegistry.register(TestModelHelper.builder(SimpleCmd.class, SimpleCmd::new)
                .name("cmd").build());

        CommandLine cmd = new CommandLine(SimpleCmd.class);
        assertThrows(UnsupportedOperationException.class,
                () -> cmd.getSubcommands().put("x", null));
    }

    // --- Error handling ---

    @Test
    void executeWithParameterErrorDefaultHandler() {
        CommandModelRegistry.register(TestModelHelper.builder(SimpleCmd.class, SimpleCmd::new)
                .name("cmd").build());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CommandLine cmd = new CommandLine(SimpleCmd.class);
        cmd.setErr(new PrintStream(baos));
        int code = cmd.execute("--invalid");
        assertEquals(ExitCode.USAGE, code);
        String errOutput = baos.toString();
        assertTrue(errOutput.contains("Error"));
        assertTrue(errOutput.contains("--help"));
    }

    @Test
    void executeWithRuntimeException() {
        CommandModelRegistry.register(TestModelHelper.builder(FailCmd.class, FailCmd::new)
                .name("fail").build());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CommandLine cmd = new CommandLine(FailCmd.class);
        cmd.setErr(new PrintStream(baos));
        int code = cmd.execute();
        assertEquals(ExitCode.SOFTWARE, code);
    }

    // --- Version via VersionProvider ---

    @Test
    void versionViaProvider() {
        CommandModelRegistry.register(TestModelHelper.builder(VersionProviderCmd.class, VersionProviderCmd::new)
                .name("vp")
                .mixinStandardHelpOptions(true)
                .versionProviderClass(TestVersionProvider.class)
                .build());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Factory factory = new Factory() {
            @Override
            public <T> T create(Class<T> cls) throws Exception {
                return cls.getDeclaredConstructor().newInstance();
            }
        };
        CommandLine cmd = new CommandLine(VersionProviderCmd.class, factory);
        cmd.setOut(new PrintStream(baos));
        assertEquals(0, cmd.execute("--version"));
        assertTrue(baos.toString().contains("TestApp 3.0.0"));
    }

    // --- Non-executable command with subcommands shows help ---

    static class NonExecCmd {
    }

    @Test
    void nonExecutableWithSubcommandsShowsHelp() {
        CommandModelRegistry.register(TestModelHelper.builder(SubCmd.class, SubCmd::new)
                .name("sub").build());
        CommandModelRegistry.register(TestModelHelper.builder(NonExecCmd.class, NonExecCmd::new)
                .name("app")
                .addSubcommand(SubCmd.class)
                .build());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CommandLine cmd = new CommandLine(NonExecCmd.class);
        cmd.setOut(new PrintStream(baos));
        int code = cmd.execute();
        assertEquals(0, code);
        assertTrue(baos.toString().contains("app"));
    }

    // --- usageHelp / versionHelp option flags ---

    @Test
    void usageHelpOption() {
        CommandModelRegistry.register(TestModelHelper.builder(SimpleCmd.class, SimpleCmd::new)
                .name("cmd")
                .addOption(new io.quarkus.quickcli.model.BuiltCommandModel.OptionBinding(
                        new String[] { "--help" }, "Show help", boolean.class,
                        false, "", "", false, "", "help",
                        false, true, false, -1,
                        io.quarkus.quickcli.model.BuiltCommandModel.FieldKind.SINGLE,
                        null, null, "", "", (inst, val) -> {
                        }))
                .build());

        CommandLine cmd = new CommandLine(SimpleCmd.class);
        ParseResult result = cmd.parse("--help");
        assertTrue(result.isHelpRequested());
    }

    @Test
    void versionHelpOption() {
        CommandModelRegistry.register(TestModelHelper.builder(SimpleCmd.class, SimpleCmd::new)
                .name("cmd")
                .version("1.0")
                .addOption(new io.quarkus.quickcli.model.BuiltCommandModel.OptionBinding(
                        new String[] { "--version" }, "Show version", boolean.class,
                        false, "", "", false, "", "version",
                        true, false, false, -1,
                        io.quarkus.quickcli.model.BuiltCommandModel.FieldKind.SINGLE,
                        null, null, "", "", (inst, val) -> {
                        }))
                .build());

        CommandLine cmd = new CommandLine(SimpleCmd.class);
        ParseResult result = cmd.parse("--version");
        assertTrue(result.isVersionRequested());
    }

    // --- CommandLine fluent API returns this ---

    @Test
    void fluentSettersReturnThis() {
        CommandModelRegistry.register(TestModelHelper.builder(SimpleCmd.class, SimpleCmd::new)
                .name("cmd").build());

        CommandLine cmd = new CommandLine(SimpleCmd.class);
        assertSame(cmd, cmd.setOut(System.out));
        assertSame(cmd, cmd.setErr(System.err));
        assertSame(cmd, cmd.setExecutionStrategy(new CommandLine.RunLastStrategy()));
        assertSame(cmd, cmd.setParameterExceptionHandler((ex, args) -> 0));
        assertSame(cmd, cmd.setExitCodeExceptionMapper(ex -> 0));
    }
}
