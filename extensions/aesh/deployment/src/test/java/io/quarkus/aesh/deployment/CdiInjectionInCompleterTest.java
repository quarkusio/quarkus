package io.quarkus.aesh.deployment;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.completer.OptionCompleter;
import org.aesh.command.container.CommandContainer;
import org.aesh.command.impl.internal.ProcessedCommand;
import org.aesh.command.impl.internal.ProcessedOption;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.aesh.runtime.AeshCdiCommandContainerBuilder;
import io.quarkus.aesh.runtime.annotations.CliCommand;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests that CDI injection works correctly in Aesh service providers
 * (completers, validators, converters, activators) that are referenced
 * via annotation attributes on {@code @Option} and {@code @Argument}.
 * <p>
 * Aesh creates these service providers via reflection, so the
 * {@link AeshCdiCommandContainerBuilder} must inject CDI dependencies
 * into them after the command container is built.
 */
public class CdiInjectionInCompleterTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(
                    TaskCommand.class,
                    DoneCommand.class,
                    ListCommand.class,
                    TaskService.class,
                    TaskNameCompleter.class));

    @Test
    public void testCompleterHasCdiInjection() throws Exception {
        AeshCdiCommandContainerBuilder<CommandInvocation> builder = new AeshCdiCommandContainerBuilder<>();
        CommandContainer<CommandInvocation> container = builder.create(TaskCommand.class);

        // Find the "done" sub-command parser
        var doneParser = container.getParser().getChildParser("done");
        Assertions.assertThat(doneParser).as("'done' sub-command parser should exist").isNotNull();

        ProcessedCommand<?, ?> processedCommand = doneParser.getProcessedCommand();

        // The @Argument has completer = TaskNameCompleter.class
        ProcessedOption argument = processedCommand.getArgument();
        Assertions.assertThat(argument).as("'done' command should have an argument").isNotNull();
        Assertions.assertThat(argument.completer()).as("argument should have a completer").isNotNull();
        Assertions.assertThat(argument.completer()).isInstanceOf(TaskNameCompleter.class);

        // Verify CDI injection worked — taskService should not be null
        TaskNameCompleter completer = (TaskNameCompleter) argument.completer();
        Assertions.assertThat(completer.taskService)
                .as("TaskService should be CDI-injected into the completer")
                .isNotNull();
    }

    @Test
    public void testOptionCompleterHasCdiInjection() throws Exception {
        AeshCdiCommandContainerBuilder<CommandInvocation> builder = new AeshCdiCommandContainerBuilder<>();
        CommandContainer<CommandInvocation> container = builder.create(TaskCommand.class);

        // Find the "list" sub-command parser
        var listParser = container.getParser().getChildParser("list");
        Assertions.assertThat(listParser).as("'list' sub-command parser should exist").isNotNull();

        ProcessedCommand<?, ?> processedCommand = listParser.getProcessedCommand();

        // The --filter option has completer = TaskNameCompleter.class
        ProcessedOption filterOption = null;
        for (ProcessedOption opt : processedCommand.getOptions()) {
            if ("filter".equals(opt.name())) {
                filterOption = opt;
                break;
            }
        }
        Assertions.assertThat(filterOption).as("'list' command should have a --filter option").isNotNull();
        Assertions.assertThat(filterOption.completer()).isInstanceOf(TaskNameCompleter.class);

        TaskNameCompleter completer = (TaskNameCompleter) filterOption.completer();
        Assertions.assertThat(completer.taskService)
                .as("TaskService should be CDI-injected into the option completer")
                .isNotNull();
    }

    @GroupCommandDefinition(name = "task", description = "Task management", groupCommands = { DoneCommand.class,
            ListCommand.class })
    @CliCommand
    public static class TaskCommand implements Command<CommandInvocation> {

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("task: use a sub-command");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "done", description = "Mark a task as done")
    public static class DoneCommand implements Command<CommandInvocation> {

        @Argument(description = "Task name", required = true, completer = TaskNameCompleter.class)
        private String taskName;

        @Inject
        TaskService taskService;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Done: " + taskName);
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "list", description = "List tasks")
    public static class ListCommand implements Command<CommandInvocation> {

        @Option(name = "filter", completer = TaskNameCompleter.class)
        private String filter;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Tasks listed");
            return CommandResult.SUCCESS;
        }
    }

    @ApplicationScoped
    public static class TaskService {

        public List<String> getPendingTaskNames() {
            return List.of("task1", "task2");
        }
    }

    @Dependent
    public static class TaskNameCompleter implements OptionCompleter<CompleterInvocation> {

        @Inject
        TaskService taskService;

        @Override
        public void complete(CompleterInvocation invocation) {
            String input = invocation.getGivenCompleteValue();
            for (String name : taskService.getPendingTaskNames()) {
                if (input == null || input.isEmpty() || name.startsWith(input)) {
                    invocation.addCompleterValue(name);
                }
            }
        }
    }
}
