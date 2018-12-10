package org.jboss.shamrock.forge;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationProperty;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Profile;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jboss.forge.addon.dependencies.Coordinate;
import org.jboss.forge.addon.dependencies.builder.CoordinateBuilder;
import org.jboss.forge.addon.facets.AbstractFacet;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.facets.constraints.FacetConstraints;
import org.jboss.forge.addon.maven.plugins.ConfigurationBuilder;
import org.jboss.forge.addon.maven.plugins.ExecutionBuilder;
import org.jboss.forge.addon.maven.plugins.MavenPluginBuilder;
import org.jboss.forge.addon.maven.projects.MavenFacet;
import org.jboss.forge.addon.maven.projects.MavenPluginFacet;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFacet;
import org.jboss.forge.addon.projects.facets.DependencyFacet;
import org.jboss.forge.addon.resource.ResourceFactory;
import org.jboss.shamrock.forge.classes.QuickstartExamples;

import javax.inject.Inject;

@FacetConstraints(@FacetConstraint(JavaSourceFacet.class))
public class ShamrockFacet extends AbstractFacet<Project> implements ProjectFacet {

    public static final String SHAMROCK_VERSION_PROPERTY_NAME = "shamrock.version";
    public static final String SHAMROCK_VERSION = "1.0.0.Alpha1-SNAPSHOT";
    private static final String SHAMROCK_VERSION_VARIABLE = "${"+ SHAMROCK_VERSION_PROPERTY_NAME +"}";
    public static final String SHAMROCK_PLUGIN_VERSION_PROPERTY_NAME = "shamrock-maven-plugin.version";
    private static final String SHAMROCK_PLUGIN_VERSION_VARIABLE = "${"+ SHAMROCK_PLUGIN_VERSION_PROPERTY_NAME +"}";
    public static final String SHAMROCK_PLUGIN_VERSION = "1.0.0.Alpha1-SNAPSHOT";

    public static final Coordinate PLUGIN_COORDINATE = CoordinateBuilder
                                                               .create().setGroupId("org.jboss.shamrock")
                                                               .setArtifactId("shamrock-maven-plugin")
                                                               .setVersion(SHAMROCK_PLUGIN_VERSION_VARIABLE);


    @Inject
    ResourceFactory resourceFactory;

    @Inject
    QuickstartExamples examples;

    @Override
    public boolean install() {
        setGroupAndArtifact();

        setShamrockVersionProperty();

        // Add dependencies
        addShamrockDependencies();

        addMavenCompilerPlugin();

        addShamrockPlugin();

        addRuntimeProfile();

        examples.createNewApplication(getFaceted(), "org.acme.quickstart", "MyApplication");

        return isInstalled();
    }

    private void setGroupAndArtifact() {
        MavenFacet maven = getFaceted().getFacet(MavenFacet.class);
        Model pom = maven.getModel();
        pom.setGroupId("org.acme.quickstart");
        pom.setArtifactId("shamrock-forge-example");
        pom.setVersion("1.0.0-SNAPSHOT");
        maven.setModel(pom);
 }

    private void addShamrockPlugin() {
        MavenPluginFacet pluginFacet = getFaceted().getFacet(MavenPluginFacet.class);
        MavenPluginBuilder builder = MavenPluginBuilder.create().setCoordinate(PLUGIN_COORDINATE);
        if (!pluginFacet.hasPlugin(PLUGIN_COORDINATE)) {
            System.out.println("Configuring the shamrock-maven-plugin...");
            builder.addExecution(ExecutionBuilder.create().addGoal("build"));
            pluginFacet.addPlugin(builder);
        }
    }

    private void addRuntimeProfile() {
        Profile profile = new Profile();
        profile.setId("native-image");
        ActivationProperty activationP = new ActivationProperty();
        activationP.setName("!no-native");
        Activation activation = new Activation();
        activation.setProperty(activationP);
        profile.setActivation(activation);

        BuildBase buildBase = new BuildBase();
        buildBase.addPlugin(createNativePlugin());
        profile.setBuild(buildBase);


        MavenFacet maven = this.getFaceted().getFacet(MavenFacet.class);
        Model pom = maven.getModel();
        List<Profile> profiles = pom.getProfiles();
        profiles.add(profile);
        pom.setProfiles(profiles);
        maven.setModel(pom);
    }

    private Plugin createNativePlugin() {
        Plugin plugin = new Plugin();
        plugin.setGroupId(PLUGIN_COORDINATE.getGroupId());
        plugin.setArtifactId(PLUGIN_COORDINATE.getArtifactId());
        plugin.setVersion(PLUGIN_COORDINATE.getVersion());

        PluginExecution execution = new PluginExecution();
        execution.setId("native-image");
        execution.addGoal("native-image");
        execution.setConfiguration(buildConfiguration());

        plugin.addExecution(execution);

        return plugin;
    }

    private Object buildConfiguration() {

        String configString = "<configuration>" +
                              "<cleanupServer>true</cleanupServer>" +
                              "<enableHttpUrlHandler>true</enableHttpUrlHandler>" +
                              "<!-- Requires Protean Graal fork to work, will fail otherwise" +
                              "<enableRetainedHeapReporting>true</enableRetainedHeapReporting> <enableCodeSizeReporting>true</enableCodeSizeReporting> -->" +
                              "<graalvmHome>${graalvmHome}</graalvmHome>" +
                              "<enableJni>false</enableJni>" +
                              "</configuration>";
        Xpp3Dom config;
        try {
            config = Xpp3DomBuilder.build(new StringReader(configString));
        }
        catch (XmlPullParserException | IOException ex) {
            throw new RuntimeException("Issue creating config for native plugin", ex);
        }
        return config;
    }

    @Override
    public boolean isInstalled() {
        MavenFacet mavenFacet = getMavenFacet();
        Model pom = mavenFacet.getModel();
        return pom.getProperties().getProperty(SHAMROCK_VERSION_PROPERTY_NAME) != null;
    }

    public MavenFacet getMavenFacet() {
        return getFaceted().getFacet(MavenFacet.class);
    }

    private void setShamrockVersionProperty() {
        ForgeUtils.addPropertyToProject(this.getFaceted(), SHAMROCK_VERSION_PROPERTY_NAME, SHAMROCK_VERSION);
        ForgeUtils.addPropertyToProject(this.getFaceted(), SHAMROCK_PLUGIN_VERSION_PROPERTY_NAME, SHAMROCK_PLUGIN_VERSION);
    }

    private void addMavenCompilerPlugin() {
        Coordinate coordinate = CoordinateBuilder.create()
                .setGroupId("org.apache.maven.plugins")
                .setArtifactId("maven-compiler-plugin");

        MavenPluginFacet pluginFacet = getFaceted().getFacet(MavenPluginFacet.class);
        MavenPluginBuilder builder = MavenPluginBuilder.create().setCoordinate(coordinate);
        if (!pluginFacet.hasPlugin(coordinate)) {
            System.out.println("Configuring the maven-compiler-plugin...");
            ConfigurationBuilder configurationBuilder = ConfigurationBuilder.create();
            builder.setConfiguration(configurationBuilder);
            configurationBuilder.createConfigurationElement("source").setText("1.8");
            configurationBuilder.createConfigurationElement("target").setText("1.8");
            configurationBuilder.createConfigurationElement("parameters").setText("true");
            pluginFacet.addPlugin(builder);
        }
    }

    private void addShamrockDependencies() {
        ForgeUtils.getOrAddDependency(getFaceted(), "org.jboss.shamrock", "shamrock-jaxrs-deployment",
                ShamrockFacet.SHAMROCK_VERSION_VARIABLE, null, "provided");
        ForgeUtils.getOrAddDependency(getFaceted(), "org.jboss.shamrock", "shamrock-arc-deployment",
                ShamrockFacet.SHAMROCK_VERSION_VARIABLE, null, "provided");
        ForgeUtils.getOrAddDependency(getFaceted(), "org.jboss.shamrock", "shamrock-logging-deployment",
                ShamrockFacet.SHAMROCK_VERSION_VARIABLE, null, "provided");

        //tests
        ForgeUtils.getOrAddDependency(getFaceted(), "org.jboss.shamrock", "shamrock-junit",
                ShamrockFacet.SHAMROCK_VERSION_VARIABLE, null, "test");

        ForgeUtils.getOrAddDependency(getFaceted(), "io.rest-assured", "rest-assured",
                "3.2.0", null, "test");
    }

    private DependencyFacet getDependencyFacet() {
        return getFaceted().getFacet(DependencyFacet.class);
    }

}
