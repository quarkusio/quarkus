package io.quarkus.cli.plugin;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import io.quarkus.cli.build.ExecuteUtil;
import io.quarkus.cli.common.OutputOptionMixin;

public interface PluginCommand extends Callable<Integer> {

    List<String> getCommand();

    List<String> getArguments();

    OutputOptionMixin getOutput();

    void useArguments(List<String> arguments);

    default Path getWorkingDirectory() {
        return Paths.get(System.getProperty("user.dir"));
    }

    default Integer call() throws Exception {
        try {
            List<String> commandWithArgs = new ArrayList<>();
            commandWithArgs.addAll(getCommand());
            commandWithArgs.addAll(getArguments());
            return ExecuteUtil.executeProcess(getOutput(), commandWithArgs.toArray(new String[commandWithArgs.size()]),
                    getWorkingDirectory().toFile());
        } catch (Exception e) {
            e.printStackTrace();
            return getOutput().handleCommandException(e, "Unable to run plugin command: [" + String.join(" ", getCommand())
                    + "] with arguments: [" + String.join(" ", getArguments()) + "]");
        }
    }
}
