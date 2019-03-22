/*
 * Copyright 2019 Red Hat, Inc.
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
package io.quarkus.gradle;

import java.io.File;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
public class QuarkusPluginExtension {

    private final Project project;

    private String transformedClassesDirectory = "transformed-classes";

    private String wiringClassesDirectory = "wiring-classes";

    private String libDir = "lib";

    private String outputDirectory;

    private String finalName;

    private String sourceDir;

    public QuarkusPluginExtension(Project project) {
        this.project = project;
    }

    public File transformedClassesDirectory() {
        return new File(project.getBuildDir() + File.separator + transformedClassesDirectory);
    }

    public void setTransformedClassesDirectory(String transformedClassesDirectory) {
        this.transformedClassesDirectory = transformedClassesDirectory;
    }

    public File outputDirectory() {
        if (outputDirectory == null)
            outputDirectory = project.getConvention().getPlugin(JavaPluginConvention.class)
                    .getSourceSets().getByName("main").getOutput().getClassesDirs().getAsPath();

        return new File(outputDirectory);
    }

    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public File sourceDir() {
        if (sourceDir == null)
            sourceDir = project.getConvention().getPlugin(JavaPluginConvention.class)
                    .getSourceSets().getByName("main").getAllJava().getSourceDirectories().getAsPath();

        return new File(sourceDir);
    }

    public void setSourceDir(String sourceDir) {
        this.sourceDir = sourceDir;
    }

    public File wiringClassesDirectory() {
        return new File(project.getBuildDir() + File.separator + wiringClassesDirectory);
    }

    public void setWiringClassesDirectory(String wiringClassesDirectory) {
        this.wiringClassesDirectory = wiringClassesDirectory;
    }

    public File libDir() {
        return new File(project.getBuildDir() + File.separator + libDir);
    }

    public void setLibDir(String libDir) {
        this.libDir = libDir;
    }

    public String finalName() {
        if (finalName == null || finalName.length() == 0)
            return project.getName();
        else
            return finalName;
    }

    public void setFinalName(String finalName) {
        this.finalName = finalName;
    }

    public String groupId() {
        return project.getGroup().toString();
    }

    public String artifactId() {
        return project.getName();
    }

    public String version() {
        return project.getVersion().toString();
    }

    public boolean uberJar() {
        return false;
    }

    public Set<File> resourcesDir() {
        return project.getConvention().getPlugin(JavaPluginConvention.class)
                .getSourceSets().getByName("main").getResources().getSrcDirs();
    }

    public Set<File> dependencyFiles() {
        SourceSet sourceSet = project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName("main");
        return sourceSet.getCompileClasspath().getFiles();
    }

}
