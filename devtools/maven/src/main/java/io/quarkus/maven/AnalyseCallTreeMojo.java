package io.quarkus.maven;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import io.quarkus.deployment.pkg.steps.ReportAnalyzer;

/**
 * Analyze call tree of a method or a class based on an existing report produced by Substrate when using
 * -H:+PrintAnalysisCallTree,
 * and does a more meaningful analysis of what is causing a type to be retained.
 */
@Mojo(name = "analyze-call-tree")
public class AnalyseCallTreeMojo extends AbstractMojo {

    @Parameter(defaultValue = "${class}")
    private String className;

    @Parameter(defaultValue = "${method}")
    private String methodName;

    @Parameter(defaultValue = "${project.build.directory}/reports")
    private File reportsDir;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (methodName != null && className != null) {
            throw new MojoFailureException("Cannot specify both class and method name");
        }
        String clazz = className;
        String method = "<init>";
        if (methodName != null) {
            int index = methodName.lastIndexOf('.');
            clazz = methodName.substring(0, index);
            method = methodName.substring(index + 1);
        }

        File[] files = reportsDir.listFiles();
        if (files == null) {
            throw new MojoFailureException("No reports in " + reportsDir);
        }
        for (File i : files) {
            if (i.getName().startsWith("call_tree")) {
                try {
                    System.out.println(new ReportAnalyzer(i.getAbsolutePath()).analyse(clazz, method));
                } catch (Exception e) {
                    throw new MojoExecutionException("Failed", e);
                }
            }
        }
    }
}
