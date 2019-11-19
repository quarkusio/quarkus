package io.quarkus.cli.commands.file;

import static io.quarkus.maven.utilities.MojoUtils.configuration;
import static io.quarkus.maven.utilities.MojoUtils.getBomGroupId;
import static io.quarkus.maven.utilities.MojoUtils.getBomArtifactId;
import static io.quarkus.maven.utilities.MojoUtils.getBomVersion;
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
import io.quarkus.generators.ProjectGenerator;
import io.quarkus.maven.utilities.MojoUtils;
import io.quarkus.maven.utilities.MojoUtils.Element;
import io.quarkus.platform.templates.BomTemplate;
import io.quarkus.platform.templates.ProjectConfigTemplate;
import io.quarkus.platform.templates.ProjectPomTemplateFactory;
import io.quarkus.platform.templates.PropertyTemplate;

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
        if(getModel() == null) {
            return;
        }
        try (ByteArrayOutputStream pomOutputStream = new ByteArrayOutputStream()) {
            MojoUtils.write(getModel(), pomOutputStream);
            write(BuildTool.MAVEN.getDependenciesFile(), pomOutputStream.toString("UTF-8"));
        }
    }

    @Override
    protected void addDependencyInBuildFile(Dependency dependency) throws IOException {
        if(getModel() != null) {
            getModel().addDependency(dependency);
        }
    }

    @Override
    protected boolean hasDependency(Extension extension) throws IOException {
        return getModel() != null && MojoUtils.hasDependency(getModel(), extension.getGroupId(), extension.getArtifactId());
    }

    @Override
    public List<Dependency> getDependencies() throws IOException {
        return getModel() == null ? Collections.emptyList() : getModel().getDependencies();
    }

    @Override
    protected boolean containsBOM() throws IOException {
        final Model model = getModel();
        if(model == null || model.getDependencyManagement() == null) {
            return false;
        }
        List<Dependency> dependencies = model.getDependencyManagement().getDependencies();
        return dependencies.stream()
                // Find bom
                .filter(dependency -> "import".equals(dependency.getScope()))
                .filter(dependency -> "pom".equals(dependency.getType()))
                // Does it match the bom artifact name
                .anyMatch(d -> d.getArtifactId().equals(MojoUtils.TEMPLATE_PROPERTY_QUARKUS_PLATFORM_ARTIFACT_ID_VALUE)
                        || d.getArtifactId().equals(getBomArtifactId()));
    }

    @Override
    public void completeFile(String groupId, String artifactId, String version) throws IOException {
        addQuarkusProperties();
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
            hasBom = containsBOM();
        }

        if (!hasBom) {
            final ProjectConfigTemplate pomTemplate = ProjectPomTemplateFactory.forPom(MojoUtils.getPomTemplate());
            if(pomTemplate == null) {
                return;
            }
            final BomTemplate bomTemplate = pomTemplate.getPlatformBom();
            if(bomTemplate == null) {
                return;
            }
            Dependency bom = new Dependency();
            bom.setGroupId(bomTemplate.getGroupId().getPropertyName() == null ? getBomGroupId() : bomTemplate.getGroupId().getPropertyExpr());
            bom.setArtifactId(bomTemplate.getArtifactId().getPropertyName() == null ? getBomArtifactId() : bomTemplate.getArtifactId().getPropertyExpr());

            String v = null;
            if(bomTemplate.getVersion().getPropertyName() == null) {
                v = getBomVersion();
                // legacy prop
                if(v.equals(MojoUtils.getQuarkusVersion())) {
                    v = MojoUtils.TEMPLATE_PROPERTY_QUARKUS_VERSION_VALUE;
                }
            } else {
                v = bomTemplate.getVersion().getPropertyExpr();
            }
            bom.setVersion(v);
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

            Plugin plg = plugin(getPluginGroupId(), getPluginArtifactId(), MojoUtils.TEMPLATE_PROPERTY_QUARKUS_PLUGIN_VERSION_VALUE);
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
            Plugin plugin = plugin(getPluginGroupId(), getPluginArtifactId(), MojoUtils.TEMPLATE_PROPERTY_QUARKUS_PLUGIN_VERSION_VALUE);
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
        if(getModel() == null) {
            return false;
        }
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
        final Build build = getModel().getBuild();
        if (build != null && build.getPluginManagement() != null) {
            if (build.getPluginManagement().getPlugins() == null) {
                build.getPluginManagement().setPlugins(new ArrayList<>());
            }
            build.getPluginManagement().getPlugins().add(plugin);
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

    private void addQuarkusProperties() throws IOException {
        final Model model = getModel();
        Properties properties = model.getProperties();
        if (properties == null) {
            properties = new Properties();
            model.setProperties(properties);
        }

        final ProjectConfigTemplate pomTemplate = ProjectPomTemplateFactory.forPom(MojoUtils.getPomTemplate());
        if(pomTemplate == null) {
            return;
        }
        for(PropertyTemplate propTemplate : pomTemplate.getProperties().values()) {
            final String propName = propTemplate.getPropertyName();
            if(propName == null) {
                continue;
            }
            switch(propName) {
                case ProjectGenerator.BOM_GROUP_ID:
                    properties.putIfAbsent(propTemplate.getName(), getBomGroupId());
                    break;
                case ProjectGenerator.BOM_ARTIFACT_ID:
                    properties.putIfAbsent(propTemplate.getName(), getBomArtifactId());
                    break;
                case ProjectGenerator.BOM_VERSION:
                    properties.putIfAbsent(propTemplate.getName(), getBomVersion());
                    break;
                case ProjectGenerator.QUARKUS_VERSION:
                    properties.putIfAbsent(propTemplate.getName(), MojoUtils.getQuarkusVersion());
                    break;
                case "plugin_version":
                    properties.putIfAbsent(propTemplate.getName(), getPluginVersion());
                    break;
            }
        }
    }

    private boolean isParentPom() throws IOException {
        return getModel() != null && "pom".equals(getModel().getPackaging());
    }

    @Override
    public List<Dependency> getManagedDependencies() throws IOException {
        if(getModel() == null) {
            return Collections.emptyList();
        }
        final DependencyManagement managed = getModel().getDependencyManagement();
        return managed != null ? managed.getDependencies()
                : Collections.emptyList();
    }

    @Override
    public String getProperty(String propertyName) throws IOException {
        if(getModel() == null) {
            return null;
        }
        return getModel().getProperties().getProperty(propertyName);
    }

    private Model getModel() throws IOException {
        if (model == null) {
            initModel();
        }
        return model;
    }
}
