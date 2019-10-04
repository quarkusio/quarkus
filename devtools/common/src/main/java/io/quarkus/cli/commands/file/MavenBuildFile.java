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
import java.util.Collections;
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
        super(writer, BuildTool.MAVEN);
    }

    private void initModel() throws IOException {
        if (getWriter().exists(BuildTool.MAVEN.getDependenciesFile())) {
            byte[] content = getWriter().getContent(BuildTool.MAVEN.getDependenciesFile());
            this.model = MojoUtils.readPom(new ByteArrayInputStream(content));
        }
    }

    @Override
    public void close() throws IOException {
        ByteArrayOutputStream pomOutputStream = new ByteArrayOutputStream();
        MojoUtils.write(getModel(), pomOutputStream);
        write(BuildTool.MAVEN.getDependenciesFile(), pomOutputStream.toString("UTF-8"));
    }

    @Override
    protected void addDependencyInBuildFile(Dependency dependency) throws IOException {
        getModel().addDependency(dependency);
    }

    @Override
    protected boolean hasDependency(Extension extension) throws IOException {
        return MojoUtils.hasDependency(getModel(), extension.getGroupId(), extension.getArtifactId());
    }

    @Override
    public List<Dependency> getDependencies() throws IOException {
        return getModel().getDependencies();
    }

    @Override
    protected boolean containsBOM() throws IOException {
        if (getModel().getDependencyManagement() == null) {
            return false;
        }
        List<Dependency> dependencies = getModel().getDependencyManagement().getDependencies();
        return dependencies.stream()
                // Find bom
                .filter(dependency -> "import".equalsIgnoreCase(dependency.getScope()))
                .filter(dependency -> "pom".equalsIgnoreCase(dependency.getType()))
                // Does it matches the bom artifact name
                .anyMatch(dependency -> dependency.getArtifactId().equalsIgnoreCase(getBomArtifactId()));
    }

    @Override
    public void completeFile(String groupId, String artifactId, String version) throws IOException {
        addVersionProperty();
        addBom();
        addMainPluginConfig();
        addNativeProfile();
    }

    private void addBom() throws IOException {
        boolean hasBom = false;
        DependencyManagement dm = getModel().getDependencyManagement();
        if (dm == null) {
            dm = new DependencyManagement();
            getModel().setDependencyManagement(dm);
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

    private void addNativeProfile() throws IOException {
        final boolean match = getModel().getProfiles().stream().anyMatch(p -> p.getId().equals("native"));
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
            getModel().addProfile(profile);
        }
    }

    private void addMainPluginConfig() throws IOException {
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

    private boolean hasPlugin() throws IOException {
        List<Plugin> plugins = null;
        final Build build = getModel().getBuild();
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

    private void addPluginManagementSection(Plugin plugin) throws IOException {
        if (getModel().getBuild() != null && getModel().getBuild().getPluginManagement() != null) {
            if (getModel().getBuild().getPluginManagement().getPlugins() == null) {
                getModel().getBuild().getPluginManagement().setPlugins(new ArrayList<>());
            }
            getModel().getBuild().getPluginManagement().getPlugins().add(plugin);
        }
    }

    private Build createBuildSectionIfRequired() throws IOException {
        Build build = getModel().getBuild();
        if (build == null) {
            build = new Build();
            getModel().setBuild(build);
        }
        if (build.getPlugins() == null) {
            build.setPlugins(new ArrayList<>());
        }
        return build;
    }

    private void addVersionProperty() throws IOException {
        Properties properties = getModel().getProperties();
        if (properties == null) {
            properties = new Properties();
            getModel().setProperties(properties);
        }
        properties.putIfAbsent("quarkus.version", getPluginVersion());
    }

    private boolean isParentPom() throws IOException {
        return "pom".equals(getModel().getPackaging());
    }

    @Override
    protected List<Dependency> getManagedDependencies() throws IOException {
        final DependencyManagement managed = getModel().getDependencyManagement();
        return managed != null ? managed.getDependencies()
                : Collections.emptyList();
    }

    @Override
    public String getProperty(String propertyName) throws IOException {
        return getModel().getProperties().getProperty(propertyName);
    }

    private Model getModel() throws IOException {
        if (model == null) {
            initModel();
        }
        return model;
    }

}
