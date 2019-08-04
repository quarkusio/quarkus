package io.quarkus.cli.commands.file;

import static io.quarkus.maven.utilities.MojoUtils.QUARKUS_VERSION_PROPERTY;
import static io.quarkus.maven.utilities.MojoUtils.configuration;
import static io.quarkus.maven.utilities.MojoUtils.getBomArtifactId;
import static io.quarkus.maven.utilities.MojoUtils.getPluginArtifactId;
import static io.quarkus.maven.utilities.MojoUtils.getPluginGroupId;
import static io.quarkus.maven.utilities.MojoUtils.getPluginVersion;
import static io.quarkus.maven.utilities.MojoUtils.plugin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

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

import io.quarkus.cli.commands.writer.ProjectWriter;
import io.quarkus.dependencies.Extension;
import io.quarkus.generators.BuildTool;
import io.quarkus.maven.utilities.MojoUtils;
import io.quarkus.maven.utilities.MojoUtils.Element;

public class MavenBuildFile extends BuildFile {

    private Model model;

    public MavenBuildFile(ProjectWriter writer) throws IOException {
        super(writer);
        byte[] content = writer.getContent(BuildTool.MAVEN.getDependenciesFile());
        this.model = MojoUtils.readPom(new ByteArrayInputStream(content));
    }

    @Override
    public void write() throws IOException {
        ByteArrayOutputStream pomOutputStream = new ByteArrayOutputStream();
        MojoUtils.write(model, pomOutputStream);
        write(BuildTool.MAVEN.getDependenciesFile(), pomOutputStream.toString("UTF-8"));
    }

    protected void addDependencyInBuildFile(Dependency dependency) {
        model.addDependency(dependency);
    }

    protected boolean hasDependency(Extension extension) {
        return MojoUtils.hasDependency(model, extension.getGroupId(), extension.getArtifactId());
    }

    @Override
    protected List<Dependency> getDependencies() {
        return model.getDependencies();
    }

    @Override
    protected boolean containsBOM() {
        if (model.getDependencyManagement() == null) {
            return false;
        }
        List<Dependency> dependencies = model.getDependencyManagement().getDependencies();
        return dependencies.stream()
                // Find bom
                .filter(dependency -> "import".equalsIgnoreCase(dependency.getScope()))
                .filter(dependency -> "pom".equalsIgnoreCase(dependency.getType()))
                // Does it matches the bom artifact name
                .anyMatch(dependency -> dependency.getArtifactId().equalsIgnoreCase(getBomArtifactId()));
    }

    public void completeFile() throws IOException {
        addVersionProperty();
        addBom();
        addMainPluginConfig();
        addNativeProfile();
        write();
    }

    private void addBom() {
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

    private void addNativeProfile() {
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

    private void addMainPluginConfig() {
        if (!hasPlugin()) {
            Build build = createBuildSectionIfRequired();
            Plugin plugin = plugin(getPluginGroupId(), getPluginArtifactId(), QUARKUS_VERSION_PROPERTY);
            if (isParentPom()) {
                addPluginManagementSection(plugin);
                //strip the quarkusVersion off
                plugin = plugin(getPluginGroupId(), getPluginArtifactId());
            }
            PluginExecution pluginExec = new PluginExecution();
            pluginExec.addGoal("build");
            plugin.addExecution(pluginExec);
            build.getPlugins().add(plugin);
        }
    }

    private boolean hasPlugin() {
        List<Plugin> plugins = null;
        final Build build = model.getBuild();
        if (build != null) {
            if (isParentPom()) {
                final PluginManagement management = build.getPluginManagement();
                if (management != null) {
                    plugins = management.getPlugins();
                }
            } else {
                plugins = build.getPlugins();
            }
        }
        return plugins != null && build.getPlugins()
                .stream()
                .anyMatch(p -> p.getGroupId().equalsIgnoreCase(getPluginGroupId()) &&
                        p.getArtifactId().equalsIgnoreCase(getPluginArtifactId()));
    }

    private void addPluginManagementSection(Plugin plugin) {
        if (model.getBuild() != null && model.getBuild().getPluginManagement() != null) {
            if (model.getBuild().getPluginManagement().getPlugins() == null) {
                model.getBuild().getPluginManagement().setPlugins(new ArrayList<>());
            }
            model.getBuild().getPluginManagement().getPlugins().add(plugin);
        }
    }

    private Build createBuildSectionIfRequired() {
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

    private void addVersionProperty() {
        Properties properties = model.getProperties();
        if (properties == null) {
            properties = new Properties();
            model.setProperties(properties);
        }
        properties.putIfAbsent("quarkus.version", getPluginVersion());
    }

    private boolean isParentPom() {
        return "pom".equals(model.getPackaging());
    }

}
