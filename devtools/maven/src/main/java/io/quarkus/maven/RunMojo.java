package io.quarkus.maven;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.deployment.builditem.DevServicesLauncherConfigResultBuildItem;
import io.quarkus.deployment.cmd.RunCommandActionResultBuildItem;
import io.quarkus.deployment.cmd.StartDevServicesAndRunCommandHandler;
import io.quarkus.runtime.LaunchMode;

@Mojo(name = "run")
public class RunMojo extends QuarkusBootstrapMojo {
    /**
     * The list of system properties defined for the plugin.
     */
    @Parameter
    Map<String, String> systemProperties = Collections.emptyMap();

    @Override
    protected boolean beforeExecute() throws MojoExecutionException, MojoFailureException {
        return true;
    }

    @Override
    protected void doExecute() throws MojoExecutionException, MojoFailureException {
        Set<String> propertiesToClear = new HashSet<>();

        // Add the system properties of the plugin to the system properties
        // if and only if they are not already set.
        for (Map.Entry<String, String> entry : systemProperties.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }

            String key = entry.getKey();
            if (System.getProperty(key) == null) {
                System.setProperty(key, entry.getValue());
                propertiesToClear.add(key);
            }
        }

        try (CuratedApplication curatedApplication = bootstrapApplication(LaunchMode.NORMAL,
                new Consumer<QuarkusBootstrap.Builder>() {
                    @Override
                    public void accept(QuarkusBootstrap.Builder builder) {
                        // we need this for dev services
                        builder.setMode(QuarkusBootstrap.Mode.TEST);
                    }
                })) {
            AugmentAction action = curatedApplication.createAugmentor();
            AtomicReference<Boolean> exists = new AtomicReference<>();
            AtomicReference<String> tooMany = new AtomicReference<>();
            String target = System.getProperty("quarkus.run.target");
            action.performCustomBuild(StartDevServicesAndRunCommandHandler.class.getName(), new Consumer<Map<String, List>>() {
                @Override
                public void accept(Map<String, List> cmds) {
                    List cmd = null;
                    if (target != null) {
                        cmd = cmds.get(target);
                        if (cmd == null) {
                            exists.set(false);
                            return;
                        }
                    } else if (cmds.size() == 1) { // defaults to pure java run
                        cmd = cmds.values().iterator().next();
                    } else if (cmds.size() == 2) { // choose not default
                        for (Map.Entry<String, List> entry : cmds.entrySet()) {
                            if (entry.getKey().equals("java"))
                                continue;
                            cmd = entry.getValue();
                            break;
                        }
                    } else if (cmds.size() > 2) {
                        tooMany.set(cmds.keySet().stream().collect(Collectors.joining(" ")));
                        return;
                    } else {
                        throw new RuntimeException("Should never reach this!");
                    }
                    List<String> args = (List<String>) cmd.get(0);
                    if (getLog().isInfoEnabled()) {
                        getLog().info("Executing \"" + String.join(" ", args) + "\"");
                    }
                    Path workingDirectory = (Path) cmd.get(1);
                    try {
                        ProcessBuilder builder = new ProcessBuilder()
                                .command(args)
                                .inheritIO();
                        if (workingDirectory != null) {
                            builder.directory(workingDirectory.toFile());
                        }
                        Process process = builder.start();
                        int exit = process.waitFor();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            },
                    RunCommandActionResultBuildItem.class.getName(), DevServicesLauncherConfigResultBuildItem.class.getName());
            if (target != null && !exists.get()) {
                getLog().error("quarkus.run.target " + target + " is not found");
                return;
            }
            if (tooMany.get() != null) {
                getLog().error(
                        "Too many installed extensions support quarkus:run.  Use -Dquarkus.run.target=<target> to choose");
                getLog().error("Extensions: " + tooMany.get());
            }
        } finally {
            // Clear all the system properties set by the plugin
            propertiesToClear.forEach(System::clearProperty);
        }
    }
}
