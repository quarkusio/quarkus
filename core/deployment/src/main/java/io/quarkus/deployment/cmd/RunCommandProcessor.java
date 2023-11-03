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
import io.quarkus.deployment.pkg.builditem.LegacyJarRequiredBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.pkg.builditem.UberJarRequiredBuildItem;

public class RunCommandProcessor {
    private static final String JAVA_HOME_SYS = "java.home";
    private static final String JAVA_HOME_ENV = "JAVA_HOME";

    @BuildStep
    public RunCommandActionResultBuildItem commands(List<RunCommandActionBuildItem> cmds) {
        return new RunCommandActionResultBuildItem(cmds);
    }

    @BuildStep
    public void defaultJavaCommand(PackageConfig packageConfig,
            OutputTargetBuildItem jar,
            List<UberJarRequiredBuildItem> uberJarRequired,
            List<LegacyJarRequiredBuildItem> legacyJarRequired,
            BuildProducer<RunCommandActionBuildItem> cmds) {

        Path jarPath = null;
        if (legacyJarRequired.isEmpty() && (!uberJarRequired.isEmpty()
                || packageConfig.type.equalsIgnoreCase(PackageConfig.UBER_JAR))) {
            jarPath = jar.getOutputDirectory()
                    .resolve(jar.getBaseName() + packageConfig.getRunnerSuffix() + ".jar");
        } else if (!legacyJarRequired.isEmpty() || packageConfig.isLegacyJar()
                || packageConfig.type.equalsIgnoreCase(PackageConfig.LEGACY)) {
            jarPath = jar.getOutputDirectory()
                    .resolve(jar.getBaseName() + packageConfig.getRunnerSuffix() + ".jar");
        } else {
            jarPath = jar.getOutputDirectory().resolve(DEFAULT_FAST_JAR_DIRECTORY_NAME).resolve(QUARKUS_RUN_JAR);

        }

        List<String> args = new ArrayList<>();
        args.add(determineJavaPath());

        for (Map.Entry e : System.getProperties().entrySet()) {
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
