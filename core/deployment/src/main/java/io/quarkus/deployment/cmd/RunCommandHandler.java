package io.quarkus.deployment.cmd;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.quarkus.builder.BuildResult;

public class RunCommandHandler implements BiConsumer<Object, BuildResult> {

    @Override
    public void accept(Object o, BuildResult buildResult) {
        RunCommandActionResultBuildItem result = buildResult.consume(RunCommandActionResultBuildItem.class);

        // FYI: AugmentAction.performCustomBuild runs in its own classloader
        // so we can only pass back instances of those classes in the system classloader

        Consumer<Map<String, List>> consumer = (Consumer<Map<String, List>>) o;
        Map<String, List> entries = new HashMap<>();
        for (RunCommandActionBuildItem item : result.getCommands()) {
            LinkedList itemList = new LinkedList();
            addLaunchCommand(itemList, item);
            entries.put(item.getCommandName(), itemList);
        }
        consumer.accept(entries);
    }

    private void addLaunchCommand(List list, RunCommandActionBuildItem item) {
        list.add(item.getArgs());
        list.add(item.getWorkingDirectory());
        list.add(item.getStartedExpression());
        list.add(item.isNeedsLogfile());
        list.add(item.getLogFile());
    }
}
