package io.quarkus.claesh;

import java.io.IOException;

import org.aesh.command.CommandException;
import org.aesh.command.CommandNotFoundException;
import org.aesh.command.Executor;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.CommandInvocationConfiguration;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.shell.Shell;
import org.aesh.command.validator.CommandValidatorException;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.readline.Prompt;
import org.aesh.readline.action.KeyAction;

public class QuarkusCommandInvocation<CI extends CommandInvocation> implements CommandInvocation {

    private final CommandInvocation<CI> delegate;
    private final QuarkusContext context;

    QuarkusCommandInvocation(CommandInvocation<CI> delegate, QuarkusContext context) {
        this.delegate = delegate;
        this.context = context;
    }

    @Override
    public Shell getShell() {
        return delegate.getShell();
    }

    @Override
    public void setPrompt(Prompt prompt) {
        delegate.setPrompt(prompt);
    }

    @Override
    public Prompt getPrompt() {
        return delegate.getPrompt();
    }

    @Override
    public String getHelpInfo(String commandName) {
        return delegate.getHelpInfo(commandName);
    }

    @Override
    public String getHelpInfo() {
        return delegate.getHelpInfo();
    }

    @Override
    public void stop() {
        delegate.stop();
    }

    @Override
    public KeyAction input() throws InterruptedException {
        return delegate.input();
    }

    @Override
    public String inputLine() throws InterruptedException {
        return delegate.inputLine();
    }

    @Override
    public String inputLine(Prompt prompt) throws InterruptedException {
        return delegate.inputLine(prompt);
    }

    @Override
    public void executeCommand(String input) throws CommandNotFoundException,
            CommandLineParserException, OptionValidatorException,
            CommandValidatorException, CommandException, InterruptedException, IOException {
        delegate.executeCommand(input);
    }

    @Override
    public void print(String msg, boolean paging) {
        delegate.print(msg, paging);
    }

    @Override
    public void println(String msg, boolean paging) {
        delegate.println(msg, paging);
    }

    @Override
    public Executor<CI> buildExecutor(String line) throws CommandNotFoundException,
            CommandLineParserException, OptionValidatorException, CommandValidatorException, IOException {
        return delegate.buildExecutor(line);
    }

    @Override
    public void print(String msg) {
        delegate.print(msg);
    }

    @Override
    public void println(String msg) {
        delegate.println(msg);
    }

    @Override
    public CommandInvocationConfiguration getConfiguration() {
        return null;
    }

    public QuarkusContext quarkusContext() {
        return context;
    }
}
