package io.quarkus.maven;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
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
import io.smallrye.common.process.ProcessBuilder;

@Mojo(name = "run")
public class RunMojo extends QuarkusBootstrapMojo {
    /**
     * The list of system properties defined for the plugin.
     */
    @Parameter
    Map<String, String> systemProperties = Collections.emptyMap();

    /**
     * Additional system properties meant to be passed on the command line in a formal like
     * {@code -DsysProps=prop1=val1,prop2=val2}
     */
    @Parameter(defaultValue = "${sysProps}")
    String additionalSystemProperties;

    /**
     * The list of environment variables with which the process will be launched. To be specified in a format like
     * {@code -DenvVars=VAR1=val1,VAR2=val2}
     */
    @Parameter(defaultValue = "${envVars}")
    String environmentVariables;

    /**
     * The list of program arguments with which the process will be launched. To be specified in a format like
     * {@code -Dargs=1,2,k=v}
     */
    @Parameter(defaultValue = "${args}")
    String programArguments;

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

        try (CuratedApplication curatedApplication = bootstrapApplication(LaunchMode.RUN,
                new Consumer<QuarkusBootstrap.Builder>() {
                    @Override
                    public void accept(QuarkusBootstrap.Builder builder) {
                        // we need this for dev services
                        builder.setMode(QuarkusBootstrap.Mode.RUN);
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
                    if (additionalSystemProperties != null) {
                        String[] props = additionalSystemProperties.split(",");
                        for (int i = props.length - 1; i >= 0; i--) {
                            String prop = props[i];
                            String[] parts = prop.split("=");
                            if (parts.length == 2) {
                                // we want to set the system property write after the command
                                args.add(1, "-D" + prop);
                            } else {
                                throw new RuntimeException("Invalid system property: " + prop);
                            }
                        }
                    }
                    if (programArguments != null) {
                        args.addAll(Arrays.asList(programArguments.split(",")));
                    }
                    if (getLog().isInfoEnabled()) {
                        getLog().info("Executing \"" + String.join(" ", args) + "\"");
                    }
                    Path workingDirectory = (Path) cmd.get(1);
                    var pb = ProcessBuilder.newBuilder(args.get(0))
                            .arguments(args.subList(1, args.size()))
                            .output().inherited()
                            .error().inherited();
                    if (workingDirectory != null) {
                        pb.directory(workingDirectory);
                    }
                    if ((environmentVariables != null) && !environmentVariables.isEmpty()) {
                        pb.environment(envVarsAsMap());
                    }
                    pb.run();
                }

                private Map<String, String> envVarsAsMap() {
                    String[] envVars = environmentVariables.split(",");
                    Map<String, String> env = new HashMap<>();
                    for (String envVar : envVars) {
                        String[] parts = envVar.split("=", 2);
                        if (parts.length == 2) {
                            env.put(parts[0], parts[1]);
                        } else {
                            throw new RuntimeException("Invalid environment variable: " + envVar);
                        }
                    }
                    return env;
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
