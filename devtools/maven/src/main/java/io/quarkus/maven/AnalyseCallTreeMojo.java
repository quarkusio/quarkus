/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.maven;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import io.quarkus.creator.phase.nativeimage.ReportAnalyzer;

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
            int idex = methodName.lastIndexOf('.');
            clazz = methodName.substring(0, idex);
            method = methodName.substring(idex + 1);
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
