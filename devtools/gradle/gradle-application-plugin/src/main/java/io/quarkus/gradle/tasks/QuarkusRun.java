package io.quarkus.gradle.tasks;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.deployment.builditem.DevServicesLauncherConfigResultBuildItem;
import io.quarkus.deployment.cmd.RunCommandActionResultBuildItem;
import io.quarkus.deployment.cmd.StartDevServicesAndRunCommandHandler;
import io.quarkus.gradle.extension.QuarkusPluginExtension;

public abstract class QuarkusRun extends QuarkusBuildTask {
    private final Property<File> workingDirectory;
    private final SourceSet mainSourceSet;
    private final ListProperty<String> jvmArgs;

    @Inject
    public QuarkusRun() {
        this("Quarkus runs target application");
    }

    public QuarkusRun(String description) {
        super(description, false);
        final ObjectFactory objectFactory = getProject().getObjects();
        mainSourceSet = getProject().getExtensions().getByType(SourceSetContainer.class)
                .getByName(SourceSet.MAIN_SOURCE_SET_NAME);

        workingDirectory = objectFactory.property(File.class);
        workingDirectory.convention(getProject().provider(() -> QuarkusPluginExtension.getLastFile(getCompilationOutput())));

        jvmArgs = objectFactory.listProperty(String.class);
    }

    /**
     * The JVM classes directory (compilation output)
     */
    @Optional
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public FileCollection getCompilationOutput() {
        return mainSourceSet.getOutput().getClassesDirs();
    }

    @Input
    public Property<File> getWorkingDirectory() {
        return workingDirectory;
    }

    /**
     * @deprecated See {@link #workingDirectory}
     */
    @Deprecated
    public void setWorkingDir(String workingDir) {
        workingDirectory.set(getProject().file(workingDir));
    }

    @Input
    public ListProperty<String> getJvmArguments() {
        return jvmArgs;
    }

    @Internal
    public List<String> getJvmArgs() {
        return jvmArgs.get();
    }

    @SuppressWarnings("unused")
    @Option(description = "Set JVM arguments", option = "jvm-args")
    public void setJvmArgs(List<String> jvmArgs) {
        this.jvmArgs.set(jvmArgs);
    }

    @TaskAction
    public void runQuarkus() {
        ApplicationModel appModel = resolveAppModelForBuild();
        Properties sysProps = new Properties();
        sysProps.putAll(extension().buildEffectiveConfiguration(appModel).getOnlyQuarkusValues());
        try (CuratedApplication curatedApplication = QuarkusBootstrap.builder()
                .setBaseClassLoader(getClass().getClassLoader())
                .setExistingModel(appModel)
                .setTargetDirectory(getProject().getLayout().getBuildDirectory().getAsFile().get().toPath())
                .setBaseName(extension().finalName())
                .setBuildSystemProperties(sysProps)
                .setAppArtifact(appModel.getAppArtifact())
                .setLocalProjectDiscovery(false)
                .setIsolateDeployment(true)
                .setMode(QuarkusBootstrap.Mode.TEST)
                .build().bootstrap()) {

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
                    if (getJvmArguments().isPresent() && !getJvmArguments().get().isEmpty()) {
                        args.addAll(1, getJvmArgs());
                    }

                    getProject().getLogger().info("Executing \"" + String.join(" ", args) + "\"");
                    Path wd = (Path) cmd.get(1);
                    File wdir = wd != null ? wd.toFile() : workingDirectory.get();

                    try {
                        // this was all very touchy to get the process outputing to console and exiting cleanly
                        // change at your own risk

                        // We cannot use getProject().exec() as contrl-c is not processed correctly
                        // and the spawned process will not shutdown
                        //
                        // This also requires running with --no-daemon as control-c doesn't seem to trigger the shutdown hook
                        // this poor gradle behavior is a long known issue with gradle
                        ProcessUtil.launch(args, wdir, getProject().getLogger());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            },
                    RunCommandActionResultBuildItem.class.getName(), DevServicesLauncherConfigResultBuildItem.class.getName());
            if (target != null && !exists.get()) {
                getProject().getLogger().error("quarkus.run.target " + target + " is not found");
                return;
            }
            if (tooMany.get() != null) {
                getProject().getLogger().error(
                        "Too many installed extensions support quarkus:run.  Use -Dquarkus.run.target=<target> to choose");
                getProject().getLogger().error("Extensions: " + tooMany.get());
            }
        } catch (BootstrapException e) {
            throw new GradleException("Failed to run application", e);
        }
    }

}
