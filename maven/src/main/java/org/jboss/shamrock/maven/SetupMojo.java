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
import org.jboss.shamrock.maven.components.Prompter;
import org.jboss.shamrock.maven.utilities.MojoUtils;
import org.jboss.shamrock.maven.utilities.SetupTemplateUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static org.jboss.shamrock.maven.utilities.MojoUtils.plugin;

/**
 * This Goal helps in setting up ShamRock Maven project with shamrock-maven-plugin, with sensible defaults
 */
@Mojo(name = "setup", requiresProject = false)
public class SetupMojo extends AbstractMojo {

    private static final String JAVA_EXTENSION = ".java";
    private static final String SHAMROCK_VERSION = "shamrock-version";
    private static final String VERSION_VAR = "${shamrock.version}";
    private static final String PLUGIN_GROUPID = "org.jboss.shamrock";
    private static final String PLUGIN_ARTIFACTID = "shamrock-maven-plugin";

    /**
     * The Maven project which will define and configure the shamrock-maven-plugin
     */
    @Parameter(defaultValue = "${project}")
    protected MavenProject project;

    @Parameter(property = "projectGroupId")
    protected String projectGroupId;

    @Parameter(property = "projectArtifactId")
    protected String projectArtifactId;

    @Parameter(property = "projectVersion", defaultValue = "1.0-SNAPSHOT")
    protected String projectVersion;

    @Parameter(property = "shamrockVersion")
    protected String shamrockVersion;

    @Parameter(property = "path", defaultValue = "/hello")
    protected String path;

    @Parameter(property = "className")
    protected String className;

    @Parameter(property = "root", defaultValue = "/app")
    protected String root;

    @Parameter(property = "dependencies")
    protected List<String> dependencies;

    @Component
    protected Prompter prompter;

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
        SetupTemplateUtils.createSource(project, root, path, className, getLog());

        Optional<Plugin> vmPlugin = MojoUtils.hasPlugin(project, PLUGIN_GROUPID + ":" + PLUGIN_ARTIFACTID);
        if (vmPlugin.isPresent()) {
            return;
        }

        // The plugin is not configured, add it.

        addShamrockVersionProperty(model);
        addMainPluginConfig(model);

        // TODO add extensions
        // addDependencies(model);

        addNativeProfile(model);
        save(pomFile, model);
    }

    private void addNativeProfile(Model model) {
        Profile profile = new Profile();
        profile.setId("native");
        BuildBase buildBase = new BuildBase();
        Plugin plg = plugin(PLUGIN_GROUPID, PLUGIN_ARTIFACTID, VERSION_VAR);
        PluginExecution exec = new PluginExecution();
        exec.addGoal("native-image");
        MojoUtils.Element element = new MojoUtils.Element("enableHttpUrlHandler", "true");
        exec.setConfiguration(new MojoUtils.Element("configuration", element).toDom());
        plg.addExecution(exec);
        buildBase.addPlugin(plg);
        profile.setBuild(buildBase);
        model.addProfile(profile);
    }

    private void addMainPluginConfig(Model model) {
        Plugin plugin = plugin(PLUGIN_GROUPID, PLUGIN_ARTIFACTID, VERSION_VAR);
        if (isParentPom(model)) {
            addPluginManagementSection(model, plugin);
            //strip the shamrockVersion off
            plugin = plugin(PLUGIN_GROUPID, PLUGIN_ARTIFACTID);
        } else {
            plugin = plugin(PLUGIN_GROUPID, PLUGIN_ARTIFACTID, VERSION_VAR);
        }
        PluginExecution pluginExec = new PluginExecution();
        pluginExec.addGoal("build");
        plugin.addExecution(pluginExec);
        Build build = createBuildSectionIfRequired(model);
        build.getPlugins().add(plugin);
    }

    private void addShamrockVersionProperty(Model model) {
        //Set  a property at maven project level for Shamrock maven plugin versions
        shamrockVersion = shamrockVersion == null ? MojoUtils.getVersion(SHAMROCK_VERSION) : shamrockVersion;
        model.getProperties().putIfAbsent("shamrock.version", shamrockVersion);
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
                            MojoUtils.getVersion(SHAMROCK_VERSION));
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
                // Need to create directory
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
            context.put("shamrockVersion", shamrockVersion != null ? shamrockVersion : MojoUtils.getVersion(SHAMROCK_VERSION));

            context.put("className", className);
            context.put("root", root);
            context.put("path", path);

            SetupTemplateUtils.createPom(context, pomFile);

            //The project should be recreated and set with right model
            MavenXpp3Reader xpp3Reader = new MavenXpp3Reader();

            model = xpp3Reader.read(new FileInputStream(pomFile));
        } catch (Exception e) {
            throw new MojoExecutionException("Error while setup of vertx-maven-plugin", e);
        }

        project = new MavenProject(model);
        project.setFile(pomFile);
        project.setPomFile(pomFile);
        project.setOriginalModel(model); // the current model is the original model as well

        // Add dependencies
//        if (addDependencies(model)) {
        save(pomFile, model);
//        }
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

    private Dependency parse(String dependency) {
        Dependency res = new Dependency();
        String[] segments = dependency.split(":");
        if (segments.length >= 2) {
            res.setGroupId(segments[0]);
            res.setArtifactId(segments[1]);
            if (segments.length >= 3 && !segments[2].isEmpty()) {
                res.setVersion(segments[2]);
            }
            if (segments.length >= 4) {
                res.setClassifier(segments[3]);
            }
            return res;
        } else {
            getLog().warn("Invalid dependency description '" + dependency + "'");
            return null;
        }
    }

//    private boolean addDependencies(Model model) {
//        if (dependencies == null || dependencies.isEmpty()) {
//            return false;
//        }
//
//        boolean updated = false;
//        List<VertxDependency> deps = VertxDependencies.get();
//        for (String dependency : this.dependencies) {
//            Optional<VertxDependency> optional = deps.stream()
//                .filter(d -> d.labels().contains(dependency.toLowerCase()))
//                .findAny();
//
//            if (optional.isPresent()) {
//                getLog().info("Adding dependency " + optional.get().toCoordinates());
//                model.addDependency(optional.get().toDependency());
//                updated = true;
//            } else if (dependency.contains(":")) {
//                // Add it as a dependency
//                // groupId:artifactId:version:classifier
//                Dependency parsed = parse(dependency);
//                if (parsed != null) {
//                    getLog().info("Adding dependency " + parsed.getManagementKey());
//                    model.addDependency(parsed);
//                    updated = true;
//                }
//            } else {
//                getLog().warn("Cannot find a dependency matching '" + dependency + "'");
//            }
//        }
//
//        return updated;
//    }


    private void createDirectories() {
        File root = project.getBasedir();
        File source = new File(root, "src/main/java");
        File resources = new File(root, "src/main/resources");
        File test = new File(root, "src/test/java");

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

    /**
     * Method used to add the vert.x dependencies typically the vert.x core
     *
     * @param model - the {@code {@link Model }}
     */
//    private void addVertxDependencies(Model model) {
//        String groupId = VERTX_GROUP_ID;
//        String artifactId = "vertx-core";
//        if (model.getDependencies() != null) {
//            if (!MojoUtils.hasDependency(project, groupId, artifactId)) {
//                model.getDependencies().add(
//                    dependency(groupId, artifactId, null));
//            }
//        } else {
//            model.setDependencies(new ArrayList<>());
//            model.getDependencies().add(dependency(groupId, artifactId, null));
//        }
//    }

}
