package io.quarkus.picocli.runtime;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Event;

import io.quarkus.runtime.QuarkusApplication;
import picocli.CommandLine;

@Dependent
public class PicocliRunner implements QuarkusApplication {

    private static final class EventExecutionStrategy implements CommandLine.IExecutionStrategy {
        private final CommandLine.IExecutionStrategy executionStrategy;
        private final Event<CommandLine.ParseResult> parseResultEvent;

        private EventExecutionStrategy(CommandLine.IExecutionStrategy executionStrategy,
                Event<CommandLine.ParseResult> parseResultEvent) {
            this.executionStrategy = executionStrategy;
            this.parseResultEvent = parseResultEvent;
        }

        @Override
        public int execute(CommandLine.ParseResult parseResult)
                throws CommandLine.ExecutionException, CommandLine.ParameterException {
            parseResultEvent.fire(parseResult);
            return executionStrategy.execute(parseResult);
        }
    }

    private final CommandLine commandLine;

    public PicocliRunner(CommandLine commandLine, Event<CommandLine.ParseResult> parseResultEvent) {
        this.commandLine = commandLine
                .setExecutionStrategy(new EventExecutionStrategy(commandLine.getExecutionStrategy(), parseResultEvent));
    }

    @Override
    public int run(String... args) throws Exception {
        try {
            return commandLine.execute(args);
        } finally {
            commandLine.getOut().flush();
            commandLine.getErr().flush();
        }
    }
}
