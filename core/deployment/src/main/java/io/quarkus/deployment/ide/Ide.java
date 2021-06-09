package io.quarkus.deployment.ide;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public enum Ide {

    IDEA("idea", "--help"),
    ECLIPSE("eclipse", (String[]) null),
    VSCODE("code", "--version"),
    NETBEANS("netbeans", "--help");

    private final String defaultCommand;
    private final List<String> markerArgs;
    private String machineSpecificCommand;

    private String effectiveCommand;

    Ide(String defaultCommand, String... markerArgs) {
        this.defaultCommand = defaultCommand;
        this.markerArgs = markerArgs != null ? Arrays.asList(markerArgs) : Collections.emptyList();
    }

    /**
     * Attempts to launch the default IDE script. If it succeeds, then that command is used (as the command is on the $PATH),
     * otherwise the full path of the command (determined earlier in the process by looking at the running processes)
     * is used.
     */
    public String getEffectiveCommand() {
        if (effectiveCommand != null) {
            return effectiveCommand;
        }
        effectiveCommand = doGetEffectiveCommand();
        return effectiveCommand;
    }

    private String doGetEffectiveCommand() {
        if (defaultCommand != null) {
            if (markerArgs == null) {
                // in this case there is nothing much we can do but hope that the default command will work
                return defaultCommand;
            } else {
                try {
                    List<String> command = new ArrayList<>(1 + markerArgs.size());
                    command.add(defaultCommand);
                    command.addAll(markerArgs);
                    new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD.file())
                            .redirectOutput(ProcessBuilder.Redirect.DISCARD.file()).start()
                            .waitFor(10,
                                    TimeUnit.SECONDS);
                    return defaultCommand;
                } catch (Exception e) {
                    return machineSpecificCommand;
                }
            }
        } else {
            // in this case the IDE does not provide a default command so we need to rely on what was found
            // from inspecting the running processes
            return machineSpecificCommand;
        }
    }

    public void setMachineSpecificCommand(String machineSpecificCommand) {
        this.machineSpecificCommand = machineSpecificCommand;
    }

    @Override
    public String toString() {
        return "Ide{" +
                "defaultCommand='" + defaultCommand + '\'' +
                '}';
    }
}
