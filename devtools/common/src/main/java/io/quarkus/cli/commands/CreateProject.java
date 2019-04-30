package io.quarkus.cli.commands;

import static io.quarkus.maven.utilities.MojoUtils.QUARKUS_VERSION_PROPERTY;
import static io.quarkus.maven.utilities.MojoUtils.configuration;
import static io.quarkus.maven.utilities.MojoUtils.getBomArtifactId;
import static io.quarkus.maven.utilities.MojoUtils.getPluginArtifactId;
import static io.quarkus.maven.utilities.MojoUtils.getPluginGroupId;
import static io.quarkus.maven.utilities.MojoUtils.getPluginVersion;
import static io.quarkus.maven.utilities.MojoUtils.plugin;
import static io.quarkus.templates.QuarkusTemplate.ADDITIONAL_GITIGNORE_ENTRIES;
import static io.quarkus.templates.QuarkusTemplate.CLASS_NAME;
import static io.quarkus.templates.QuarkusTemplate.PACKAGE_NAME;
import static io.quarkus.templates.QuarkusTemplate.PROJECT_ARTIFACT_ID;
import static io.quarkus.templates.QuarkusTemplate.PROJECT_GROUP_ID;
import static io.quarkus.templates.QuarkusTemplate.PROJECT_VERSION;
import static io.quarkus.templates.QuarkusTemplate.QUARKUS_VERSION;
import static io.quarkus.templates.QuarkusTemplate.SOURCE_TYPE;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationProperty;
import org.apache.maven.model.Build;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Profile;

import io.quarkus.cli.commands.writer.Writer;
import io.quarkus.maven.utilities.MojoUtils;
import io.quarkus.maven.utilities.MojoUtils.Element;
import io.quarkus.templates.BuildTool;
import io.quarkus.templates.SourceType;
import io.quarkus.templates.TemplateRegistry;
import io.quarkus.templates.rest.BasicRest;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
public class CreateProject {

    private static final String POM_PATH = "/pom.xml";
    private Writer writer;
    private String groupId;
    private String artifactId;
    private String version = getPluginVersion();
    private SourceType sourceType = SourceType.JAVA;
    private BuildTool buildTool = BuildTool.MAVEN;
    private String className;

    private Model model;

    public CreateProject(final Writer writer) {
        this.writer = writer;
    }

    public CreateProject groupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    public CreateProject artifactId(String artifactId) {
        this.artifactId = artifactId;
        return this;
    }

    public CreateProject version(String version) {
        this.version = version;
        return this;
    }

    public CreateProject sourceType(SourceType sourceType) {
        this.sourceType = sourceType;
        return this;
    }

    public CreateProject className(String className) {
        this.className = className;
        return this;
    }

    public CreateProject buildTool(BuildTool buildTool) {
        this.buildTool = buildTool;
        return this;
    }

    public Model getModel() {
        return model;
    }

    public boolean doCreateProject(final Map<String, Object> context) throws IOException {
        if (!writer.init()) {
            return false;
        }

        MojoUtils.getAllProperties().forEach((k, v) -> context.put(k.replace("-", "_"), v));

        context.put(PROJECT_GROUP_ID, groupId);
        context.put(PROJECT_ARTIFACT_ID, artifactId);
        context.put(PROJECT_VERSION, version);
        context.put(QUARKUS_VERSION, getPluginVersion());
        context.put(SOURCE_TYPE, sourceType);
        context.put(ADDITIONAL_GITIGNORE_ENTRIES, buildTool.getGitIgnoreEntries());

        if (className != null) {
            className = sourceType.stripExtensionFrom(className);
            int idx = className.lastIndexOf('.');
            if (idx >= 0) {
                final String packageName = className.substring(0, idx);
                className = className.substring(idx + 1);
                context.put(PACKAGE_NAME, packageName);
            }
            context.put(CLASS_NAME, className);
        }

        TemplateRegistry.createTemplateWith(BasicRest.TEMPLATE_NAME).generate(writer, context);

        final byte[] pom = writer.getContent(POM_PATH);
        model = MojoUtils.readPom(new ByteArrayInputStream(pom));
        addVersionProperty(model);
        addBom(model);
        addMainPluginConfig(model);
        addNativeProfile(model);
        ByteArrayOutputStream pomOutputStream = new ByteArrayOutputStream();
        MojoUtils.write(model, pomOutputStream);
        writer.write(POM_PATH, pomOutputStream.toString());

        return true;
    }

    private void addBom(Model model) {
        boolean hasBom = false;
        DependencyManagement dm = model.getDependencyManagement();
        if (dm == null) {
            dm = new DependencyManagement();
            model.setDependencyManagement(dm);
        } else {
            hasBom = dm.getDependencies().stream()
                    .anyMatch(d -> d.getGroupId().equals(getPluginGroupId()) &&
                            d.getArtifactId().equals(getBomArtifactId()));
        }

        if (!hasBom) {
            Dependency bom = new Dependency();
            bom.setGroupId(getPluginGroupId());
            bom.setArtifactId(getBomArtifactId());
            bom.setVersion(QUARKUS_VERSION_PROPERTY);
            bom.setType("pom");
            bom.setScope("import");

            dm.addDependency(bom);
        }
    }

    private void addNativeProfile(Model model) {
        final boolean match = model.getProfiles().stream().anyMatch(p -> p.getId().equals("native"));
        if (!match) {
            PluginExecution exec = new PluginExecution();
            exec.addGoal("native-image");
            exec.setConfiguration(configuration(new Element("enableHttpUrlHandler", "true")));

            Plugin plg = plugin(getPluginGroupId(), getPluginArtifactId(), QUARKUS_VERSION_PROPERTY);
            plg.addExecution(exec);

            BuildBase buildBase = new BuildBase();
            buildBase.addPlugin(plg);

            Profile profile = new Profile();
            profile.setId("native");
            profile.setBuild(buildBase);

            final Activation activation = new Activation();
            final ActivationProperty property = new ActivationProperty();
            property.setName("native");

            activation.setProperty(property);
            profile.setActivation(activation);
            model.addProfile(profile);
        }
    }

    private void addMainPluginConfig(Model model) {
        if (!hasPlugin(model)) {
            Plugin plugin = plugin(getPluginGroupId(), getPluginArtifactId(), QUARKUS_VERSION_PROPERTY);
            if (isParentPom(model)) {
                addPluginManagementSection(model, plugin);
                //strip the quarkusVersion off
                plugin = plugin(getPluginGroupId(), getPluginArtifactId());
            }
            PluginExecution pluginExec = new PluginExecution();
            pluginExec.addGoal("build");
            plugin.addExecution(pluginExec);
            Build build = createBuildSectionIfRequired(model);
            build.getPlugins().add(plugin);
        }
    }

    private boolean hasPlugin(final Model model) {
        List<Plugin> plugins = null;
        if (isParentPom(model)) {
            final PluginManagement management = model.getBuild().getPluginManagement();
            if (management != null) {
                plugins = management.getPlugins();
            }
        } else {
            final Build build = model.getBuild();
            if (build != null) {
                plugins = build.getPlugins();
            }
        }
        return plugins != null && model.getBuild().getPlugins()
                .stream()
                .anyMatch(p -> p.getGroupId().equalsIgnoreCase(getPluginGroupId()) &&
                        p.getArtifactId().equalsIgnoreCase(getPluginArtifactId()));
    }

    private void addPluginManagementSection(Model model, Plugin plugin) {
        if (model.getBuild().getPluginManagement() != null) {
            if (model.getBuild().getPluginManagement().getPlugins() == null) {
                model.getBuild().getPluginManagement().setPlugins(new ArrayList<>());
            }
            model.getBuild().getPluginManagement().getPlugins().add(plugin);
        }
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

    private void addVersionProperty(Model model) {
        Properties properties = model.getProperties();
        if (properties == null) {
            properties = new Properties();
            model.setProperties(properties);
        }
        properties.putIfAbsent("quarkus.version", getPluginVersion());
    }

    private boolean isParentPom(Model model) {
        return "pom".equals(model.getPackaging());
    }

    public static SourceType determineSourceType(Set<String> extensions) {
        return extensions.stream().anyMatch(e -> e.toLowerCase().contains("kotlin"))
                ? SourceType.KOTLIN
                : SourceType.JAVA;
    }
}
