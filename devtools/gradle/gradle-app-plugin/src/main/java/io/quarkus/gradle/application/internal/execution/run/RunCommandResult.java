package io.quarkus.gradle.application.internal.execution.run;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record RunCommandResult(Map<String, List<?>> commands, Runnable closeDevServices) {

    public RunCommandResult {
        commands = Map.copyOf(commands);
    }

    @SuppressWarnings("unchecked")
    public Map<String, RunCommand> runCommands() {
        Map<String, RunCommand> result = new HashMap<>();
        commands.forEach((name, command) -> {
            List<String> arguments = (List<String>) command.get(0);
            var workingDirectory = (Path) command.get(1);
            var startedExpression = (String) command.get(2);
            boolean needsLogfile = (Boolean) command.get(3);
            var logFile = (Path) command.get(4);
            result.put(name, new RunCommand(name, arguments, Optional.ofNullable(workingDirectory),
                    Optional.ofNullable(startedExpression), needsLogfile, Optional.ofNullable(logFile)));
        });
        return result;
    }
}
