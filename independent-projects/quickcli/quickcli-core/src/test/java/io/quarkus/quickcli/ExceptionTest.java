package io.quarkus.quickcli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.Test;

import io.quarkus.quickcli.model.CommandModelRegistry;

class ExceptionTest {

    static class Cmd implements Runnable {
        @Override
        public void run() {
        }
    }

    // --- ParameterException ---

    @Test
    void parameterExceptionMessageOnly() {
        var ex = new CommandLine.ParameterException("bad param");
        assertEquals("bad param", ex.getMessage());
        assertNull(ex.getCommandLine());
    }

    @Test
    void parameterExceptionWithCommandLine() {
        CommandModelRegistry.register(TestModelHelper.builder(Cmd.class, Cmd::new)
                .name("cmd").build());

        CommandLine cmd = new CommandLine(Cmd.class);
        var ex = new CommandLine.ParameterException("bad param", cmd);
        assertEquals("bad param", ex.getMessage());
        assertSame(cmd, ex.getCommandLine());
    }

    // --- UnmatchedArgumentException ---

    @Test
    void unmatchedArgumentException() {
        var ex = new CommandLine.UnmatchedArgumentException("unmatched arg");
        assertEquals("unmatched arg", ex.getMessage());
        assertNull(ex.getCommandLine());
    }

    @Test
    void unmatchedArgumentExceptionWithCommandLine() {
        CommandModelRegistry.register(TestModelHelper.builder(Cmd.class, Cmd::new)
                .name("cmd").build());

        CommandLine cmd = new CommandLine(Cmd.class);
        var ex = new CommandLine.UnmatchedArgumentException("unmatched", cmd);
        assertSame(cmd, ex.getCommandLine());
    }

    @Test
    void printSuggestions() {
        CommandModelRegistry.register(TestModelHelper.builder(Cmd.class, Cmd::new)
                .name("cmd").build());

        // Register a subcommand so suggestions have something to print
        CommandModelRegistry.register(TestModelHelper.builder(Cmd.class, Cmd::new)
                .name("sub").build());

        CommandLine cmd = new CommandLine(Cmd.class);
        cmd.addSubcommand("sub", new CommandLine(Cmd.class));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        var ex = new CommandLine.UnmatchedArgumentException("unknown", cmd);
        CommandLine.UnmatchedArgumentException.printSuggestions(ex, new PrintStream(baos));
        String output = baos.toString();
        assertTrue(output.contains("Possible solutions"));
        assertTrue(output.contains("sub"));
    }

    @Test
    void printSuggestionsNoCommandLine() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        var ex = new CommandLine.ParameterException("no cmd");
        // Should not throw even without a CommandLine
        CommandLine.UnmatchedArgumentException.printSuggestions(ex, new PrintStream(baos));
        assertEquals("", baos.toString());
    }

    // --- MutuallyExclusiveArgsException ---

    @Test
    void mutuallyExclusiveArgsException() {
        var ex = new CommandLine.MutuallyExclusiveArgsException("exclusive");
        assertEquals("exclusive", ex.getMessage());
        assertInstanceOf(CommandLine.ParameterException.class, ex);
    }

    @Test
    void mutuallyExclusiveArgsExceptionWithCommandLine() {
        CommandModelRegistry.register(TestModelHelper.builder(Cmd.class, Cmd::new)
                .name("cmd").build());

        CommandLine cmd = new CommandLine(Cmd.class);
        var ex = new CommandLine.MutuallyExclusiveArgsException("exclusive", cmd);
        assertSame(cmd, ex.getCommandLine());
    }

    // --- TypeConversionException ---

    @Test
    void typeConversionException() {
        var ex = new CommandLine.TypeConversionException("bad type");
        assertEquals("bad type", ex.getMessage());
        assertInstanceOf(CommandLine.ParameterException.class, ex);
    }

    // --- ExecutionException ---

    @Test
    void executionException() {
        var cause = new RuntimeException("inner");
        var ex = new CommandLine.ExecutionException(cause);
        assertSame(cause, ex.getCause());
    }
}
