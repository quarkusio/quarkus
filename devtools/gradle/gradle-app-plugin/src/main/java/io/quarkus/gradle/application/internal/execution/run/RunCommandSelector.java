package io.quarkus.gradle.application.internal.execution.run;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.gradle.api.GradleException;

public final class RunCommandSelector {

    public RunCommand select(Map<String, RunCommand> commands, Optional<String> runTarget) {
        if (commands.isEmpty()) {
            throw new GradleException("Quarkus run did not produce any run commands");
        }
        if (runTarget.isPresent()) {
            RunCommand command = commands.get(runTarget.get());
            if (command == null) {
                throw new GradleException("Quarkus run target '" + runTarget.get()
                        + "' was not produced. Available run targets: " + commandNames(commands));
            }
            return command;
        }
        if (commands.size() == 1) {
            return commands.values().iterator().next();
        }
        if (commands.size() == 2 && commands.containsKey("java")) {
            return commands.entrySet().stream()
                    .filter(entry -> !"java".equals(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElseThrow();
        }
        throw new GradleException("Multiple Quarkus run targets were produced: " + commandNames(commands)
                + ". Configure quarkus.run.target to choose one.");
    }

    public RunCommand withArguments(RunCommand command, List<String> jvmArguments, List<String> applicationArguments) {
        List<String> arguments = new ArrayList<>(command.arguments());
        if ("java".equals(command.name()) && !jvmArguments.isEmpty()) {
            int jarIndex = arguments.indexOf("-jar");
            arguments.addAll(jarIndex > 0 ? jarIndex : 1, jvmArguments);
        }
        arguments.addAll(applicationArguments);
        return new RunCommand(command.name(), arguments, command.workingDirectory(), command.startedExpression(),
                command.needsLogfile(), command.logFile());
    }

    private static String commandNames(Map<String, RunCommand> commands) {
        return commands.keySet().stream()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.joining(", "));
    }
}
