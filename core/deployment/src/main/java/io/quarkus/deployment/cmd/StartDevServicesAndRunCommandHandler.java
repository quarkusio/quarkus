package io.quarkus.deployment.cmd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.quarkus.builder.BuildResult;
import io.quarkus.deployment.builditem.DevServicesLauncherConfigResultBuildItem;

public class StartDevServicesAndRunCommandHandler implements BiConsumer<Object, BuildResult> {

    @Override
    public void accept(Object o, BuildResult buildResult) {
        var runCommandActionResult = buildResult.consume(RunCommandActionResultBuildItem.class);
        var devServicesLauncherConfigResult = buildResult.consume(DevServicesLauncherConfigResultBuildItem.class);

        // FYI: AugmentAction.performCustomBuild runs in its own classloader
        // so we can only pass back instances of those classes in the system classloader

        Consumer<Map<String, List>> consumer = (Consumer<Map<String, List>>) o;

        // build up the commands
        Map<String, List> cmds = new HashMap<>();
        for (RunCommandActionBuildItem item : runCommandActionResult.getCommands()) {
            List<String> itemList = new ArrayList<>();
            addLaunchCommand(itemList, item, devServicesLauncherConfigResult.getConfig());
            cmds.put(item.getCommandName(), itemList);
        }

        consumer.accept(cmds);
    }

    private void addLaunchCommand(List list, RunCommandActionBuildItem item, Map<String, String> devServicesProperties) {
        List<String> effectiveArgs;
        List<String> originalArgs = item.getArgs();
        if (devServicesProperties.isEmpty()) {
            effectiveArgs = originalArgs;
        } else {
            // here we want to "inject" our dev services configuration into the predetermined launch command

            effectiveArgs = new ArrayList<>(originalArgs.size() + devServicesProperties.size());
            int jarArgIndex = -1;
            for (int i = 0; i < originalArgs.size(); i++) {
                if (originalArgs.get(i).trim().equals("-jar")) {
                    jarArgIndex = i;
                    break;
                }
            }
            if (jarArgIndex == -1) {
                effectiveArgs = originalArgs;
            } else {
                effectiveArgs.addAll(originalArgs.subList(0, jarArgIndex));
                for (var devServiceConfigEntry : devServicesProperties.entrySet()) {
                    effectiveArgs.add("-D" + devServiceConfigEntry.getKey() + "=" + devServiceConfigEntry.getValue());
                }
                effectiveArgs.addAll(originalArgs.subList(jarArgIndex, originalArgs.size()));
            }
        }

        list.add(effectiveArgs);
        list.add(item.getWorkingDirectory());
        list.add(item.getStartedExpression());
        list.add(item.isNeedsLogfile());
        list.add(item.getLogFile());
    }
}
