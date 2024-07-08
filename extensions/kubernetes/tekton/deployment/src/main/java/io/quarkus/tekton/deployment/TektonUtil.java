
package io.quarkus.tekton.deployment;

import java.io.File;
import java.util.Optional;

import io.dekorate.BuildImage;
import io.dekorate.project.BuildInfo;
import io.dekorate.project.FileProjectFactory;
import io.dekorate.project.Project;
import io.dekorate.utils.Jvm;

public class TektonUtil {

    private static final String GIT = "git";
    private static final String REVISION = "revision";
    private static final String URL = "url";
    private static final String IMAGE = "image";
    private static final String BUILD = "build";
    private static final String DEPLOY = "deploy";
    private static final String WORKSPACE = "workspace";
    private static final String RUN = "run";
    private static final String NOW = "now";
    private static final String JAVA = "java";
    private static final String DASH = "-";
    private static final String AND = "and";

    public static Optional<BuildImage> getBuildImage(File projectDir) {
        Project project = FileProjectFactory.create(projectDir);
        BuildInfo buildInfo = project.getBuildInfo();
        return BuildImage.find(buildInfo.getBuildTool(), buildInfo.getBuildToolVersion(), Jvm.getVersion(), null);
    }

    public static final String outputImageResourceName(TektonConfig config) {
        return config.name.get() + DASH + IMAGE;
    }

    public static final String imageBuildTaskName(TektonConfig config) {
        return config.name.get() + DASH + IMAGE + DASH + BUILD;
    }

    public static final String javaBuildTaskName(TektonConfig config) {
        return config.name.get() + DASH + JAVA + DASH + BUILD;
    }

    public static final String javaBuildStepName(TektonConfig config) {
        return JAVA + DASH + BUILD;
    }

    public static final String deployTaskName(TektonConfig config) {
        return config.name.get() + DASH + DEPLOY;
    }

    public static final String monolithTaskName(TektonConfig config) {
        return config.name.get() + DASH + BUILD + DASH + AND + DASH + DEPLOY;
    }
}
