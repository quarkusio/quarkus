package io.quarkus.deployment.cmd;

import static io.quarkus.deployment.pkg.steps.JarResultBuildStep.DEFAULT_FAST_JAR_DIRECTORY_NAME;
import static io.quarkus.deployment.pkg.steps.JarResultBuildStep.QUARKUS_RUN_JAR;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;

public class RunCommandProcessor {
    private static final String JAVA_HOME_SYS = "java.home";
    private static final String JAVA_HOME_ENV = "JAVA_HOME";

    @BuildStep
    public RunCommandActionResultBuildItem commands(List<RunCommandActionBuildItem> cmds) {
        return new RunCommandActionResultBuildItem(cmds);
    }

    @SuppressWarnings("deprecation") // legacy jar
    @BuildStep
    public void defaultJavaCommand(PackageConfig packageConfig,
            OutputTargetBuildItem jar,
            BuildProducer<RunCommandActionBuildItem> cmds,
            BuildSystemTargetBuildItem buildSystemTarget) {

        Path jarPath = switch (packageConfig.jar().type()) {
            case UBER_JAR -> jar.getOutputDirectory()
                    .resolve(jar.getBaseName() + packageConfig.computedRunnerSuffix() + ".jar");
            // todo: legacy JAR should be using runnerSuffix()
            case LEGACY_JAR -> jar.getOutputDirectory()
                    .resolve(jar.getBaseName() + packageConfig.computedRunnerSuffix() + ".jar");
            case FAST_JAR, MUTABLE_JAR -> jar.getOutputDirectory()
                    .resolve(DEFAULT_FAST_JAR_DIRECTORY_NAME).resolve(QUARKUS_RUN_JAR);
        };

        List<String> args = new ArrayList<>();
        args.add(determineJavaPath());

        for (Map.Entry<?, ?> e : buildSystemTarget.getBuildSystemProps().entrySet()) {
            args.add("-D" + e.getKey().toString() + "=" + e.getValue().toString());
        }
        args.add("-jar");
        args.add(jarPath.toAbsolutePath().toString());
        cmds.produce(new RunCommandActionBuildItem("java", args, null, null, null, false));
    }

    private String determineJavaPath() {
        // try system property first - it will be the JAVA_HOME used by the current JVM
        String home = System.getProperty(JAVA_HOME_SYS);
        if (home == null) {
            // No luck, somewhat a odd JVM not enforcing this property
            // try with the JAVA_HOME environment variable
            home = System.getenv(JAVA_HOME_ENV);
        }
        if (home != null) {
            File javaHome = new File(home);
            File file = new File(javaHome, "bin/java");
            if (file.exists()) {
                return file.getAbsolutePath();
            }
        }

        // just assume 'java' is on the system path
        return "java";
    }

}
