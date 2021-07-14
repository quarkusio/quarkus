package io.quarkus.deployment.ide;

import java.io.File;
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

import org.jboss.logging.Logger;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.dev.spi.DevModeType;

public class IdeProcessor {

    private static final Logger log = Logger.getLogger(IdeProcessor.class);

    private static Map<String, List<Ide>> IDE_MARKER_FILES = new HashMap<>();
    private static Map<Predicate<ProcessInfo>, Ide> IDE_PROCESSES = new HashMap<>();
    private static Map<Ide, Function<ProcessInfo, String>> IDE_ARGUMENTS_EXEC_INDICATOR = new HashMap<>();

    static {
        IDE_MARKER_FILES.put(".idea", Collections.singletonList(Ide.IDEA));
        IDE_MARKER_FILES.put(".project", Arrays.asList(Ide.VSCODE, Ide.ECLIPSE));
        IDE_MARKER_FILES.put("nbactions.xml", Collections.singletonList(Ide.NETBEANS));
        IDE_MARKER_FILES.put("nb-configuration.xml", Collections.singletonList(Ide.NETBEANS));

        IDE_MARKER_FILES = Collections.unmodifiableMap(IDE_MARKER_FILES);

        IDE_PROCESSES.put((processInfo -> processInfo.containInCommand("idea") && processInfo.command.endsWith("java")),
                Ide.IDEA);
        IDE_PROCESSES.put((processInfo -> processInfo.containInCommand("code")), Ide.VSCODE);
        IDE_PROCESSES.put((processInfo -> processInfo.containInCommand("eclipse")), Ide.ECLIPSE);
        IDE_PROCESSES.put(
                (processInfo -> processInfo.containInArguments("netbeans")),
                Ide.NETBEANS);

        IDE_ARGUMENTS_EXEC_INDICATOR.put(Ide.NETBEANS, (ProcessInfo processInfo) -> {
            String platform = processInfo.getArgumentThatContains("nbexec");
            if (platform != null && !platform.isEmpty()) {
                platform = platform.substring(0, platform.indexOf("platform")).concat("bin").concat(File.separator);
                if (IdeUtil.isWindows()) {
                    platform = platform.concat("netbeans.exe");
                } else {
                    platform = platform.concat("netbeans");
                }
                return platform;
            }
            return null;
        });
        IDE_ARGUMENTS_EXEC_INDICATOR.put(Ide.IDEA, (ProcessInfo processInfo) -> {
            // converts something like '/home/test/software/idea/ideaIU-x.y.z/idea-IU-x.y.z/jbr/bin/java ....'
            // into '/home/test/software/idea/ideaIU-203.5981.114/idea-IU-203.5981.114/bin/idea.sh'
            String command = processInfo.getCommand();
            int jbrIndex = command.indexOf("jbr");
            if ((jbrIndex > -1) && command.endsWith("java")) {
                String ideaHome = command.substring(0, jbrIndex);
                return (ideaHome + "bin" + File.separator + "idea") + (IdeUtil.isWindows() ? ".exe" : ".sh");
            } else if (IdeUtil.isWindows() && (command.endsWith("idea.exe") || command.endsWith("idea64.exe"))) {
                // based on input from https://github.com/quarkusio/quarkus/issues/18676#issuecomment-879775007
                return command;
            }
            return null;
        });

        IDE_PROCESSES = Collections.unmodifiableMap(IDE_PROCESSES);
    }

    @BuildStep
    public EffectiveIdeBuildItem effectiveIde(LaunchModeBuildItem launchModeBuildItem, IdeConfig ideConfig,
            IdeFileBuildItem ideFile,
            IdeRunningProcessBuildItem ideRunningProcess) {
        if (launchModeBuildItem.getDevModeType().orElse(null) != DevModeType.LOCAL) {
            return null;
        }
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
    public IdeFileBuildItem detectIdeFiles(LaunchModeBuildItem launchModeBuildItem,
            BuildSystemTargetBuildItem buildSystemTarget) {
        if (launchModeBuildItem.getDevModeType().orElse(null) != DevModeType.LOCAL) {
            return null;
        }
        Set<Ide> result = new HashSet<>(2);
        Path projectRoot = buildSystemTarget.getOutputDirectory().getParent();
        IDE_MARKER_FILES.forEach((file, ides) -> {
            if (Files.exists(projectRoot.resolve(file))) {
                result.addAll(ides);
            }
        });
        if (result.isEmpty()) {
            // hack to try and guess the IDE when using a multi-module project
            Path parent = projectRoot.getParent();
            IDE_MARKER_FILES.forEach((file, ides) -> {
                if (Files.exists(parent.resolve(file))) {
                    result.addAll(ides);
                }
            });
        }
        return new IdeFileBuildItem(result);
    }

    @BuildStep
    public IdeRunningProcessBuildItem detectRunningIdeProcesses(LaunchModeBuildItem launchModeBuildItem) {
        if (launchModeBuildItem.getDevModeType().orElse(null) != DevModeType.LOCAL) {
            return null;
        }
        Set<Ide> result = new HashSet<>(4);
        List<ProcessInfo> processInfos = Collections.emptyList();
        try {
            processInfos = ProcessUtil.runningProcesses();
        } catch (Exception e) {
            // this shouldn't be a terminal failure, so just log it to the console
            log.warn(e.getMessage());
        }
        for (ProcessInfo processInfo : processInfos) {
            for (Map.Entry<Predicate<ProcessInfo>, Ide> entry : IDE_PROCESSES.entrySet()) {
                if (entry.getKey().test(processInfo)) {
                    Ide ide = entry.getValue();
                    if (IDE_ARGUMENTS_EXEC_INDICATOR.containsKey(ide)) {
                        Function<ProcessInfo, String> execIndicator = IDE_ARGUMENTS_EXEC_INDICATOR.get(ide);
                        String machineSpecificCommand = execIndicator.apply(processInfo);
                        if (machineSpecificCommand != null) {
                            ide.setMachineSpecificCommand(machineSpecificCommand);
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
         */
        public static List<ProcessInfo> runningProcesses() {
            List<ProcessInfo> result = new ArrayList<>();
            ProcessHandle.allProcesses().forEach(p -> {
                ProcessHandle.Info info = p.info();
                Optional<String> command = info.command();
                if (command.isPresent()) {
                    result.add(new ProcessInfo(command.get(), info.arguments().orElse(null)));
                }
            });
            return result;
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

        private boolean containInCommand(String value) {
            return this.command.contains(value);
        }

        private boolean containInArguments(String value) {
            if (arguments != null) {
                for (String argument : arguments) {
                    if (argument.contains(value)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private String getArgumentThatContains(String contain) {
            if (arguments != null) {
                for (String argument : arguments) {
                    if (argument.contains(contain)) {
                        return argument;
                    }
                }
            }
            return null;
        }
    }
}
