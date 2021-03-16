package io.quarkus.deployment.ide;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.runtime.util.JavaVersionUtil;

public class IdeProcessor {

    private static Map<String, List<Ide>> IDE_MARKER_FILES = new HashMap<>();
    private static Map<Predicate<ProcessInfo>, Ide> IDE_PROCESSES = new HashMap<>();
    private static Map<Ide, Function<ProcessInfo, String>> IDE_ARGUMENTS_EXEC_INDICATOR = new HashMap<>();

    static {
        IDE_MARKER_FILES.put(".idea", Collections.singletonList(Ide.IDEA));
        IDE_MARKER_FILES.put(".project", Arrays.asList(Ide.VSCODE, Ide.ECLIPSE));
        IDE_MARKER_FILES.put("nbactions.xml", Collections.singletonList(Ide.NETBEANS));
        IDE_MARKER_FILES.put("nb-configuration.xml", Collections.singletonList(Ide.NETBEANS));

        IDE_MARKER_FILES = Collections.unmodifiableMap(IDE_MARKER_FILES);

        IDE_PROCESSES.put((processInfo -> processInfo.containInCommand("idea")), Ide.IDEA);
        IDE_PROCESSES.put((processInfo -> processInfo.containInCommand("code")), Ide.VSCODE);
        IDE_PROCESSES.put((processInfo -> processInfo.containInCommand("eclipse")), Ide.ECLIPSE);
        IDE_PROCESSES.put(
                (processInfo -> processInfo.containInCommandWithArgument("java", "netbeans")),
                Ide.NETBEANS);

        IDE_ARGUMENTS_EXEC_INDICATOR.put(Ide.NETBEANS, (ProcessInfo processInfo) -> {
            String platform = processInfo.getArgumentValue("-Dnetbeans.home");
            if (platform != null && !platform.isEmpty()) {
                String os = System.getProperty("os.name");
                if (os.startsWith("Windows") || os.startsWith("windows")) {
                    platform = platform.replace("platform", "bin/netbeans.exe");
                } else {
                    platform = platform.replace("platform", "bin/netbeans");
                }
                return platform;
            }
            return null;
        });

        IDE_PROCESSES = Collections.unmodifiableMap(IDE_PROCESSES);
    }

    @BuildStep
    public EffectiveIdeBuildItem effectiveIde(IdeConfig ideConfig, IdeFileBuildItem ideFile,
            IdeRunningProcessBuildItem ideRunningProcess) {
        Ide result = null;
        if (ideConfig.target == IdeConfig.Target.auto) {

            // the idea here is to auto-detect the special files that IDEs create
            // and also the running IDE process if need be

            if (ideFile.getDetectedIDEs().size() == 1) {
                result = ideFile.getDetectedIDEs().iterator().next();
            } else {
                Set<Ide> runningIdes = ideRunningProcess.getDetectedIDEs();
                if (runningIdes.size() == 1) {
                    result = runningIdes.iterator().next();
                } else {
                    List<Ide> matches = new ArrayList<>();
                    for (Ide file : ideFile.getDetectedIDEs()) {
                        for (Ide process : runningIdes) {
                            if (file == process) {
                                matches.add(file);
                            }
                        }
                    }
                    if (matches.size() == 1) {
                        result = matches.get(0);
                    }
                }
            }
        } else {
            if (ideConfig.target == IdeConfig.Target.idea) {
                result = Ide.IDEA;
            } else if (ideConfig.target == IdeConfig.Target.eclipse) {
                result = Ide.ECLIPSE;
            } else if (ideConfig.target == IdeConfig.Target.vscode) {
                result = Ide.VSCODE;
            } else if (ideConfig.target == IdeConfig.Target.netbeans) {
                result = Ide.NETBEANS;
            }
        }

        if (result == null) {
            return null;
        }

        return new EffectiveIdeBuildItem(result);
    }

    @BuildStep
    public IdeFileBuildItem detectIdeFiles(BuildSystemTargetBuildItem buildSystemTarget) {
        Set<Ide> result = new HashSet<>(2);
        Path projectRoot = buildSystemTarget.getOutputDirectory().getParent();
        IDE_MARKER_FILES.forEach((file, ides) -> {
            if (Files.exists(projectRoot.resolve(file))) {
                result.addAll(ides);
            }
        });
        return new IdeFileBuildItem(result);
    }

    @BuildStep
    public IdeRunningProcessBuildItem detectRunningIdeProcesses() {
        Set<Ide> result = new HashSet<>(4);
        List<ProcessInfo> processInfos = ProcessUtil.runningProcesses();
        for (ProcessInfo processInfo : processInfos) {
            for (Map.Entry<Predicate<ProcessInfo>, Ide> entry : IDE_PROCESSES.entrySet()) {
                if (entry.getKey().test(processInfo)) {
                    Ide ide = entry.getValue();

                    if (IDE_ARGUMENTS_EXEC_INDICATOR.containsKey(ide)) {
                        Function<ProcessInfo, String> execIndicator = IDE_ARGUMENTS_EXEC_INDICATOR.get(ide);
                        String executeLine = execIndicator.apply(processInfo);
                        if (executeLine != null) {
                            ide.setExecutable(executeLine);
                        }
                    }
                    result.add(ide);
                    break;
                }
            }
        }
        return new IdeRunningProcessBuildItem(result);
    }

    // TODO: remove when we move to Java 11 and just call the methods of 'java.lang.ProcessHandle'
    private static class ProcessUtil {

        /**
         * Returns a list of running processes
         * Only works for Java 11+
         */
        @SuppressWarnings({ "rawtypes", "unchecked" })
        public static List<ProcessInfo> runningProcesses() {
            if (!JavaVersionUtil.isJava11OrHigher()) {
                return Collections.emptyList();
            }
            // we can't use ProcessHandle directly as it is Java 9+, so we need to do reflection to the get the info we need
            try {
                Class processHandlerClass = Class.forName("java.lang.ProcessHandle");
                Method allProcessesMethod = processHandlerClass.getMethod("allProcesses");
                Method processHandleInfoMethod = processHandlerClass.getMethod("info");
                Class processHandleInfoClass = Class.forName("java.lang.ProcessHandle$Info");
                Method processHandleInfoCommandMethod = processHandleInfoClass.getMethod("command");
                Method processHandleInfoArgumentsMethod = processHandleInfoClass.getMethod("arguments");
                Stream<Object> allProcessesResult = (Stream<Object>) allProcessesMethod.invoke(null);
                List<ProcessInfo> result = new ArrayList<>();
                allProcessesResult.forEach(o -> {
                    try {
                        Object processHandleInfo = processHandleInfoMethod.invoke(o);
                        Optional<String> command = (Optional<String>) processHandleInfoCommandMethod.invoke(processHandleInfo);
                        if (command.isPresent()) {
                            Optional<String[]> arguments = (Optional<String[]>) processHandleInfoArgumentsMethod
                                    .invoke(processHandleInfo);
                            result.add(new ProcessInfo(command.get(), arguments.orElse(null)));
                        }
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                });
                return result;
            } catch (Exception e) {
                throw new RuntimeException("Unable to determine running processes", e);
            }
        }
    }

    private static class ProcessInfo {
        // the executable pathname of the process.
        private final String command;
        private final String[] arguments;

        public ProcessInfo(String command, String[] arguments) {
            this.command = command;
            this.arguments = arguments;
        }

        public String getCommand() {
            return command;
        }

        public String[] getArguments() {
            return arguments;
        }

        public boolean containInCommandWithArgument(String command, String argument) {
            return containInCommand(command) && containInArguments(argument);
        }

        public boolean containInCommand(String value) {
            return this.command.contains(value);
        }

        public boolean containInArguments(String value) {
            if (arguments != null) {
                for (String argument : arguments) {
                    if (argument.contains(value)) {
                        return true;
                    }
                }
            }
            return false;
        }

        public String getArgumentValue(String argumentKey) {
            if (arguments != null) {
                for (String argument : arguments) {
                    if (argument.startsWith(argumentKey) && argument.contains("=")) {
                        return argument.substring(argument.indexOf("=") + 1);
                    }
                }
            }
            return null;
        }
    }
}
