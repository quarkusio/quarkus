/*
 *
 *   Copyright (c) 2016-2018 Red Hat, Inc.
 *
 *   Red Hat licenses this file to you under the Apache License, version
 *   2.0 (the "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *   implied.  See the License for the specific language governing
 *   permissions and limitations under the License.
 */

package org.jboss.shamrock.maven;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.fusesource.jansi.Ansi;
import org.jboss.shamrock.cli.commands.AddExtensions;
import org.jboss.shamrock.cli.commands.CreateProject;
import org.jboss.shamrock.maven.components.Prompter;
import org.jboss.shamrock.maven.utilities.MojoUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * This goal helps in setting up Shamrock Maven project with shamrock-maven-plugin, with sensible defaults
 */
@Mojo(name = "create", requiresProject = false)
public class CreateProjectMojo extends AbstractMojo {

    public static final String PLUGIN_KEY = MojoUtils.getPluginGroupId() + ":" + MojoUtils.getPluginArtifactId();

    private static final String DEFAULT_GROUP_ID = "org.acme.shamrock.sample";
    /**
     * FQCN of the generated resources when applied on a project with an existing `pom.xml` file and the user
     * does not pass the `className` parameter.
     */
    private static final String DEFAULT_CLASS_NAME = DEFAULT_GROUP_ID + ".HelloResource";

    @Parameter(defaultValue = "${project}")
    protected MavenProject project;

    @Parameter(property = "projectGroupId")
    private String projectGroupId;

    @Parameter(property = "projectArtifactId")
    private String projectArtifactId;

    @Parameter(property = "projectVersion")
    private String projectVersion;

    @Parameter(property = "path")
    private String path;

    @Parameter(property = "className")
    private String className;

    @Parameter(property = "extensions")
    private List<String> extensions;

    @Parameter(defaultValue = "${session}")
    private MavenSession session;

    @Component
    private Prompter prompter;

    @Override
    public void execute() throws MojoExecutionException {
        File projectRoot = new File(".");
        File pom = new File(projectRoot, "pom.xml");

        if (pom.isFile()) {
            // Enforce that the GAV are not set
            if (! StringUtils.isBlank(projectGroupId) || ! StringUtils.isBlank(projectArtifactId)
                    || ! StringUtils.isBlank(projectVersion)) {
                throw new MojoExecutionException("Unable to generate the project, the `projectGroupId`, " +
                        "`projectArtifactId` and `projectVersion` parameters are not supported when applied to an " +
                        "existing `pom.xml` file");
            }

            // Load the GAV from the existing project
            projectGroupId = project.getGroupId();
            projectArtifactId = project.getArtifactId();
            projectVersion = project.getVersion();

        } else {
            askTheUserForMissingValues();
            if (! isDirectoryEmpty(projectRoot)) {
                projectRoot = new File(projectArtifactId);
                if (projectRoot.exists()) {
                    throw new MojoExecutionException("Unable to create the project - the current directory is not empty and" +
                            " the directory " + projectArtifactId + " exists");
                }
            }
        }

        boolean success;
        try {
            sanitizeOptions();

            final Map<String, Object> context = new HashMap<>();
            context.put("className", className);
            context.put("path", path);

            success = new CreateProject(projectRoot)
                    .groupId(projectGroupId)
                    .artifactId(projectArtifactId)
                    .version(projectVersion)
                    .doCreateProject(context);

            if (success) {
                new AddExtensions(new File(projectRoot, "pom.xml"))
                        .addExtensions(extensions);
            }
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
        if (success) {
            printUserInstructions(projectRoot);
        }
    }


    private void askTheUserForMissingValues() throws MojoExecutionException {

        // If the user has disabled the interactive mode or if the user has specified the artifactId, disable the
        // user interactions.
        if (! session.getRequest().isInteractiveMode()  || shouldUseDefaults()) {
            // Inject default values in all non-set parameters
            if (StringUtils.isBlank(projectGroupId)) {
                projectGroupId = DEFAULT_GROUP_ID;
            }
            if (StringUtils.isBlank(projectArtifactId)) {
                projectArtifactId = "my-shamrock-project";
            }
            if (StringUtils.isBlank(projectVersion)) {
                projectVersion = "1.0-SNAPSHOT";
            }
            return;
        }

        try {
            if (StringUtils.isBlank(projectGroupId)) {
                projectGroupId = prompter.promptWithDefaultValue("Set the project groupId",
                        DEFAULT_GROUP_ID);
            }

            if (StringUtils.isBlank(projectArtifactId)) {
                projectArtifactId = prompter.promptWithDefaultValue("Set the project artifactId",
                        "my-shamrock-project");
            }

            if (StringUtils.isBlank(projectVersion)) {
                projectVersion = prompter.promptWithDefaultValue("Set the Shamrock version",
                        "1.0-SNAPSHOT");
            }

            if (StringUtils.isBlank(className)) {
                // Ask the user if he want to create a resource
                String answer = prompter.promptWithDefaultValue("Do you want to create a REST resource? (y/n)", "no");
                if (isTrueOrYes(answer)) {
                    String defaultResourceName = projectGroupId.replace("-", ".")
                            .replace("_", ".") + ".HelloResource";
                    className = prompter.promptWithDefaultValue("Set the resource classname", defaultResourceName);
                    if (StringUtils.isBlank(path)) {
                        path = prompter.promptWithDefaultValue("Set the resource path ", "/hello");
                    }
                } else {
                    className = null;
                    path = null;
                }
            }


        } catch (IOException e) {
            throw new MojoExecutionException("Unable to get user input", e);
        }
    }

    private boolean shouldUseDefaults() {
        // Must be called before user input
        return projectArtifactId != null;

    }

    private boolean isTrueOrYes(String answer) {
        if (answer == null) {
            return false;
        }
        String content = answer.trim().toLowerCase();
        return "true".equalsIgnoreCase(content) || "yes".equalsIgnoreCase(content) || "y".equalsIgnoreCase(content);
    }

    private void sanitizeOptions() {
        // If className is null, we won't create the REST resource,
        if (className != null) {
            if (className.endsWith(MojoUtils.JAVA_EXTENSION)) {
                className = className.substring(0, className.length() - MojoUtils.JAVA_EXTENSION.length());
            }

            if (!className.contains(".")) {
                // No package name, inject one
                className = projectGroupId.replace("-", ".").replace("_", ".") + "." + className;
            }

            if (StringUtils.isBlank(path)) {
                path = "/hello";
            }

            if (!path.startsWith("/")) {
                path = "/" + path;
            }
        }
    }

    private void printUserInstructions(File root) {
        getLog().info("");
        getLog().info("========================================================================================");
        getLog().info(ansi().a("Your new application has been created in ").bold().a(root.getAbsolutePath()).boldOff().toString());
        getLog().info(ansi().a("Navigate into this directory and launch your application with ")
                .bold()
                .fg(Ansi.Color.CYAN)
                .a("mvn compile shamrock:dev")
                .reset()
                .toString());
        getLog().info(
                ansi().a("Your application will be accessible on ").bold().fg(Ansi.Color.CYAN).a("http://localhost:8080").reset().toString());
        getLog().info("========================================================================================");
        getLog().info("");
    }

    private boolean isDirectoryEmpty(File dir) {
        if (! dir.isDirectory()) {
            throw new IllegalArgumentException("The specified file must be a directory: " + dir.getAbsolutePath());
        }

        String[] children = dir.list();
        if (children == null) {
            // IO Issue
            throw new IllegalArgumentException("The specified directory cannot be accessed: " + dir.getAbsolutePath());
        }

        return children.length == 0;
    }
}
