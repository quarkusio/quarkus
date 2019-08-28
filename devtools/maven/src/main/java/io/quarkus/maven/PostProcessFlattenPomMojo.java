package io.quarkus.maven;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import io.quarkus.bootstrap.resolver.maven.workspace.ModelUtils;

/**
 * This mojo is post-processing a POM file that has been flattened by org.codehaus.mojo:flatten-maven-plugin.
 *
 * <p>
 * Specifically, it makes sure that:
 *
 * <li>if the parent POM was removed from the flattened POM, the properties used in the flattened POM are defined
 * in its properties section (by reading the raw flattened POM collecting all the properties from it and checking if
 * they are defined locally. If a property is not defined locally, the plugin will attempt to resolve
 * the property using the parent POMs and if successful, it will add the property to the flattened POM.);
 *
 * <li>the properties from the plugin configuration are added to the flattened POM;
 *
 * <li>the flattening-related plugins are removed from the flattened POM.
 */
@Mojo(name = "post-process-flattened-pom", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class PostProcessFlattenPomMojo extends AbstractMojo {

    public static class DefinedProperty {
        public String name;
        public String value;
        public String property;
    }

    public static final String GOAL_NAME = "post-process-flattened-pom";

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Parameter(defaultValue = ".flattened-pom.xml", required = false)
    private String flattenedPom;

    @Parameter(required = false)
    private Set<String> excludes = Collections.emptySet();

    @Parameter(required = false)
    private List<DefinedProperty> defines = Collections.emptyList();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        final File file = new File(project.getBasedir(), flattenedPom);
        if (!file.exists()) {
            getLog().warn("Flattened POM " + file + " does not exist");
            return;
        }

        Collection<String> definedNames;
        final int definedTotal = defines.size();
        if (definedTotal == 0) {
            definedNames = Collections.emptyList();
        } else {
            if (definedTotal < 4) {
                definedNames = new ArrayList<>(definedTotal);
            } else {
                definedNames = new HashSet<>(definedTotal);
            }
            for (DefinedProperty prop : defines) {
                definedNames.add(prop.name);
            }
        }

        final Path flattenedPom = file.toPath();
        final Model flattenedRawModel;
        Model updatedModel = null;
        try {
            flattenedRawModel = ModelUtils.readModel(flattenedPom);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to parse " + file, e);
        }

        // if the parent was removed then make sure the properties defined in parent POMs
        // are re-defined in the flattened POM
        if (flattenedRawModel.getParent() == null) {
            final Set<String> pomPropNames = new HashSet<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line = reader.readLine();
                while (line != null) {
                    collectProps(line, pomPropNames, definedNames);
                    line = reader.readLine();
                }
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to read " + file, e);
            }

            // add missing properties
            final Properties presentProps = flattenedRawModel.getProperties();
            for (String name : pomPropNames) {
                if (presentProps.containsKey(name)) {
                    continue;
                }
                final String value = project.getModel().getProperties().getProperty(name);
                if (value == null) {
                    getLog().warn("Property " + name + " has not been initialized in parent POMs");
                    continue;
                }
                if (updatedModel == null) {
                    updatedModel = flattenedRawModel.clone();
                }
                updatedModel.addProperty(name, value);
                debug("Added property %s", name);
            }
        }

        // add properties from the plugin config
        if (!defines.isEmpty()) {
            if (updatedModel == null) {
                updatedModel = flattenedRawModel.clone();
            }
            for (DefinedProperty prop : defines) {
                if (prop.value != null) {
                    updatedModel.addProperty(prop.name, prop.value);
                } else if (prop.property != null) {
                    updatedModel.addProperty(prop.name, "${" + prop.property + "}");
                }
            }
        }

        // remove flattening plugins
        final Map<String, Plugin> plugins = flattenedRawModel.getBuild().getPluginsAsMap();
        final String flattenPlugin = "org.codehaus.mojo:flatten-maven-plugin";
        if (plugins.containsKey(flattenPlugin)) {
            if (updatedModel == null) {
                updatedModel = flattenedRawModel.clone();
            }
            final Plugin removed = updatedModel.getBuild().getPluginsAsMap().remove(flattenPlugin);
            updatedModel.getBuild().removePlugin(removed);
        }
        final String quarkusPlugin = "io.quarkus:quarkus-maven-plugin";
        if (plugins.containsKey(quarkusPlugin)) {
            if (updatedModel == null) {
                updatedModel = flattenedRawModel.clone();
            }
            final Plugin plugin = updatedModel.getBuild().getPluginsAsMap().get(quarkusPlugin);
            final Iterator<PluginExecution> i = plugin.getExecutions().iterator();
            while (i.hasNext()) {
                final PluginExecution execution = i.next();
                final List<String> goals = execution.getGoals();
                if (goals.contains(GOAL_NAME)) {
                    execution.removeGoal(GOAL_NAME);
                    if (goals.isEmpty()) {
                        i.remove();
                    }
                }
            }
            if (plugin.getExecutions().isEmpty()) {
                updatedModel.getBuild().removePlugin(plugin);
            }
        }

        if (updatedModel == null) {
            return;
        }

        try {
            ModelUtils.persistModel(flattenedPom, updatedModel);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to persist updated model to " + file, e);
        }
    }

    private void debug(String msg, Object... args) {
        if (!getLog().isDebugEnabled()) {
            return;
        }
        if (args.length == 0) {
            getLog().debug(msg);
            return;
        }
        getLog().debug(String.format(msg, args));
    }

    /**
     * Collects properties referenced in the string
     *
     * @param str the string
     * @param propNames set to add the collected properties to
     * @param definedNames property names defined in the plugin configuration
     */
    private void collectProps(String str, Set<String> propNames, Collection<String> definedNames) {
        boolean sawStart = false;
        StringBuilder buf = new StringBuilder();
        int i = 0;
        while (i < str.length()) {
            final char c = str.charAt(i++);
            switch (c) {
                case '$':
                    if (sawStart) {
                        throw new IllegalStateException("'$' can't appear in a property name in '" + str + "' at index " + i);
                    }
                    if (i < str.length() && str.charAt(i) == '{') {
                        sawStart = true;
                        ++i;
                    }
                    break;
                case '}':
                    if (sawStart && buf.length() > 0) {
                        final String name = buf.toString();
                        if (!name.startsWith("project.")
                                && !name.startsWith("settings.")
                                && !name.startsWith("env.")
                                && !excludes.contains(name)
                                && !definedNames.contains(name)
                                && propNames.add(name)) {
                            debug("Found property %s", name);
                        }
                        buf.setLength(0);
                        sawStart = false;
                    }
                    break;
                default:
                    if (sawStart) {
                        buf.append(c);
                    }
            }
        }
    }
}
