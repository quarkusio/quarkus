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

import org.apache.maven.model.*;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.fusesource.jansi.Ansi;
import org.jboss.shamrock.maven.components.Prompter;
import org.jboss.shamrock.maven.components.SetupTemplates;
import org.jboss.shamrock.maven.utilities.MojoUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static org.fusesource.jansi.Ansi.ansi;
import static org.jboss.shamrock.maven.components.dependencies.Extensions.addExtensions;
import static org.jboss.shamrock.maven.utilities.MojoUtils.configuration;
import static org.jboss.shamrock.maven.utilities.MojoUtils.plugin;

/**
 * This goal helps in setting up Shamrock Maven project with shamrock-maven-plugin, with sensible defaults
 */
@Mojo(name = "create", requiresProject = false)
public class CreateProjectMojo extends AbstractMojo {

    private static final String JAVA_EXTENSION = ".java";
    public static final String VERSION_PROP = "shamrock-version";
    public static final String PLUGIN_VERSION_PROPERTY_NAME = "shamrock.version";
    public static final String PLUGIN_VERSION_PROPERTY = "${" + PLUGIN_VERSION_PROPERTY_NAME + "}";
    public static final String PLUGIN_GROUPID = "org.jboss.shamrock";
    public static final String PLUGIN_ARTIFACTID = "shamrock-maven-plugin";
    public static final String PLUGIN_KEY = PLUGIN_GROUPID + ":" + PLUGIN_ARTIFACTID;

    /**
     * The Maven project which will define and configure the shamrock-maven-plugin
     */
    @Parameter(defaultValue = "${project}")
    protected MavenProject project;

    @Parameter(property = "projectGroupId")
    private String projectGroupId;

    @Parameter(property = "projectArtifactId")
    private String projectArtifactId;

    @Parameter(property = "projectVersion", defaultValue = "1.0-SNAPSHOT")
    private String projectVersion;

    @Parameter(property = "shamrockVersion")
    private String shamrockVersion;

    @Parameter(property = "path", defaultValue = "/hello")
    protected String path;

    @Parameter(property = "className")
    private String className;

    @Parameter(property = "root", defaultValue = "/app")
    private String root;

    @Parameter(property = "extensions")
    private List<String> extensions;

    @Component
    private Prompter prompter;

    @Component
    private SetupTemplates templates;

    @Override
    public void execute() throws MojoExecutionException {
        File pomFile = project.getFile();

        Model model;
        //Create pom.xml if not
        if (pomFile == null || !pomFile.isFile()) {
            pomFile = createPomFileFromUserInputs();
        }

        //We should get cloned of the OriginalModel, as project.getModel will return effective model
        model = project.getOriginalModel().clone();

        createDirectories();
        templates.generate(project, model, root, path, className, getLog());
        Optional<Plugin> maybe = MojoUtils.hasPlugin(project, PLUGIN_KEY);

        if (maybe.isPresent()) {
            printUserInstructions(pomFile);
            return;
        }

        // The plugin is not configured, add it.
        addVersionProperty(model);
        addMainPluginConfig(model);
        addExtensions(model, extensions, getLog());
        addNativeProfile(model);
        save(pomFile, model);
    }

    private void printUserInstructions(File pomFile) {
        getLog().info("");
        getLog().info("========================================================================================");
        getLog().info(ansi().a("Your new application has been created in ").bold().a(pomFile.getAbsolutePath()).boldOff().toString());
        getLog().info(ansi().a("Navigate into this directory and launch your application with ").bold().fg(Ansi.Color.CYAN).a("mvn compile shamrock:dev").reset().toString());
        getLog().info(ansi().a("Your application will be accessible on ").bold().fg(Ansi.Color.CYAN).a("http://localhost:8080").reset().toString());
        getLog().info("========================================================================================");
        getLog().info("");
    }

    private void addNativeProfile(Model model) {
        Profile profile = new Profile();
        profile.setId("native");
        BuildBase buildBase = new BuildBase();
        Plugin plg = plugin(PLUGIN_GROUPID, PLUGIN_ARTIFACTID, PLUGIN_VERSION_PROPERTY);
        PluginExecution exec = new PluginExecution();
        exec.addGoal("native-image");
        MojoUtils.Element element = new MojoUtils.Element("enableHttpUrlHandler", "true");
        exec.setConfiguration(configuration(element));
        plg.addExecution(exec);
        buildBase.addPlugin(plg);
        profile.setBuild(buildBase);
        model.addProfile(profile);
    }

    private void addMainPluginConfig(Model model) {
        Plugin plugin = plugin(PLUGIN_GROUPID, PLUGIN_ARTIFACTID, PLUGIN_VERSION_PROPERTY);
        if (isParentPom(model)) {
            addPluginManagementSection(model, plugin);
            //strip the shamrockVersion off
            plugin = plugin(PLUGIN_GROUPID, PLUGIN_ARTIFACTID);
        } else {
            plugin = plugin(PLUGIN_GROUPID, PLUGIN_ARTIFACTID, PLUGIN_VERSION_PROPERTY);
        }
        PluginExecution pluginExec = new PluginExecution();
        pluginExec.addGoal("build");
        plugin.addExecution(pluginExec);
        Build build = createBuildSectionIfRequired(model);
        build.getPlugins().add(plugin);
    }

    private void addVersionProperty(Model model) {
        //Set  a property at maven project level for Shamrock maven plugin versions
        shamrockVersion = shamrockVersion == null ? MojoUtils.get(VERSION_PROP) : shamrockVersion;
        model.getProperties().putIfAbsent(PLUGIN_VERSION_PROPERTY_NAME, shamrockVersion);
    }

    private Build createBuildSectionIfRequired(Model model) {
        Build build = model.getBuild();
        if (build == null) {
            build = new Build();
            model.setBuild(build);
        }
        if (build.getPlugins() == null) {
            build.setPlugins(new ArrayList<>());
        }
        return build;
    }

    private void addPluginManagementSection(Model model, Plugin plugin) {
        if (model.getBuild().getPluginManagement() != null) {
            if (model.getBuild().getPluginManagement().getPlugins() == null) {
                model.getBuild().getPluginManagement().setPlugins(new ArrayList<>());
            }
            model.getBuild().getPluginManagement().getPlugins().add(plugin);
        }
    }

    private File createPomFileFromUserInputs() throws MojoExecutionException {
        Model model;
        String workingdDir = System.getProperty("user.dir");
        File pomFile = new File(workingdDir, "pom.xml");
        try {

            if (projectGroupId == null) {
                projectGroupId = prompter.promptWithDefaultValue("Set the project groupId",
                        "io.jboss.shamrock.sample");
            }

            // If the user does not specify the artifactId, we switch to the interactive mode.
            if (projectArtifactId == null) {
                projectArtifactId = prompter.promptWithDefaultValue("Set the project artifactId",
                        "my-shamrock-project");

                // Ask for version only if we asked for the artifactId
                projectVersion = prompter.promptWithDefaultValue("Set the project version", "1.0-SNAPSHOT");

                // Ask for maven version if not set
                if (shamrockVersion == null) {
                    shamrockVersion = prompter.promptWithDefaultValue("Set the Shamrock version",
                            MojoUtils.get(VERSION_PROP));
                }

                if (className == null) {
                    className = prompter.promptWithDefaultValue("Set the resource class name",
                            projectGroupId.replace("-", ".").replace("_", ".")
                                    + ".HelloResource");

                    if (className != null && className.endsWith(JAVA_EXTENSION)) {
                        className = className.substring(0, className.length() - JAVA_EXTENSION.length());
                    }
                }

                if (root == null) {
                    root = prompter.promptWithDefaultValue("Set the application root ",
                            "/app");
                    if (!root.startsWith("/")) {
                        root = "/" + root;
                    }
                }

                if (path == null) {
                    path = prompter.promptWithDefaultValue("Set the resource path ",
                            "/hello");
                    if (!path.startsWith("/")) {
                        path = "/" + path;
                    }
                }
            }

            // Create directory if the current one is not empty.
            File wkDir = new File(workingdDir);
            String[] children = wkDir.list();
            if (children != null && children.length != 0) {
                // Need to generate directory
                File sub = new File(wkDir, projectArtifactId);
                sub.mkdirs();
                getLog().info("Directory " + projectArtifactId + " created");
                // This updates the project pom file but also the base directory.
                pomFile = new File(sub, "pom.xml");
                project.setFile(pomFile);
            }


            Map<String, String> context = new HashMap<>();
            context.put("mProjectGroupId", projectGroupId);
            context.put("mProjectArtifactId", projectArtifactId);
            context.put("mProjectVersion", projectVersion);
            context.put("shamrockVersion", shamrockVersion != null ? shamrockVersion : MojoUtils.get(VERSION_PROP));
            context.put("docRoot", MojoUtils.get("doc-root"));

            context.put("className", className);
            context.put("root", root);
            context.put("path", path);

            templates.createNewProjectPomFile(context, pomFile);
            templates.createIndexPage(context, project.getBasedir(), getLog());
            templates.createConfiguration(project.getBasedir(), getLog());

            //The project should be recreated and set with right model
            MavenXpp3Reader xpp3Reader = new MavenXpp3Reader();

            model = xpp3Reader.read(new FileInputStream(pomFile));
        } catch (Exception e) {
            throw new MojoExecutionException("Error while setup of shamrock-maven-plugin", e);
        }

        project = new MavenProject(model);
        project.setFile(pomFile);
        project.setPomFile(pomFile);
        project.setOriginalModel(model); // the current model is the original model as well

        addExtensions(model, extensions, getLog());
        save(pomFile, model);
        return pomFile;
    }

    private void save(File pomFile, Model model) throws MojoExecutionException {
        MavenXpp3Writer xpp3Writer = new MavenXpp3Writer();
        try (FileWriter pomFileWriter = new FileWriter(pomFile)) {
            xpp3Writer.write(pomFileWriter, model);
            pomFileWriter.flush();
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to write the pom.xml file", e);
        }
    }

    private void createDirectories() {
        File base = project.getBasedir();
        File source = new File(base, "src/main/java");
        File resources = new File(base, "src/main/resources");
        File test = new File(base, "src/test/java");

        String prefix = "Creation of ";
        if (!source.isDirectory()) {
            boolean res = source.mkdirs();
            getLog().debug(prefix + source.getAbsolutePath() + " : " + res);
        }
        if (!resources.isDirectory()) {
            boolean res = resources.mkdirs();
            getLog().debug(prefix + resources.getAbsolutePath() + " : " + res);
        }
        if (!test.isDirectory()) {
            boolean res = test.mkdirs();
            getLog().debug(prefix + test.getAbsolutePath() + " : " + res);
        }
    }

    private boolean isParentPom(Model model) {
        return "pom".equals(model.getPackaging());
    }

}
