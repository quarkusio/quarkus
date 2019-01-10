package org.jboss.shamrock.cli.commands;


import org.aesh.utils.Config;
import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationProperty;
import org.apache.maven.model.Build;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Profile;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
public class CreateProject {

    private final String groupId;
    private final String artifactId;
    private final String version;

    private final String SHAMROCK_VERSION = "1.0.0.Alpha1-SNAPSHOT";
    public static final String SHAMROCK_VERSION_PROPERTY_NAME = "shamrock.version";
    private static final String SHAMROCK_VERSION_VARIABLE = "${"+ SHAMROCK_VERSION_PROPERTY_NAME +"}";

    public CreateProject(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    public boolean doCreateProject(File root) {
        if(root.exists() && !root.isDirectory()) {
            System.out.println("Project root needs to either not exist or be a directory");
            return false;
        }
        else if(!root.exists()) {
            boolean mkdirStatus = root.mkdirs();
            if(!mkdirStatus) {
                System.out.println("Failed to create root directory");
                return false;
            }
        }

        File pom = new File(root+ Config.getPathSeparator()+"pom.xml");

        try {
            createPom(pom);
        }
        catch(IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    private boolean createPom(File pom) throws IOException {

        Model model = new Model();
        Writer writer = new FileWriter(pom);

        setGroupArtifactAndVersion(model);

        setProperties(model);

        addMavenCompilerPlugin(model);

        addShamrockDependencies(model);

        addRuntimeProfile(model);

        new MavenXpp3Writer().write(writer, model );
        writer.close();


        return true;
    }

    private void addMavenCompilerPlugin(Model model) {
       Build build = new Build();

       Plugin mavenCompilerPlugin = new Plugin();
       mavenCompilerPlugin.setArtifactId("maven-compiler-plugin");

        final Xpp3Dom config = new Xpp3Dom("configuration");
        final Xpp3Dom source = new Xpp3Dom("source");
        source.setValue("1.8");
        final Xpp3Dom target = new Xpp3Dom("target");
        target.setValue("1.8");
        config.addChild(source);
        config.addChild(target);

        mavenCompilerPlugin.setConfiguration(config);
        build.addPlugin(mavenCompilerPlugin);

        Plugin shamrockMavenPlugin = new Plugin();
        shamrockMavenPlugin.setGroupId("org.jboss.shamrock");
        shamrockMavenPlugin.setArtifactId("shamrock-maven-plugin");
        shamrockMavenPlugin.setVersion(SHAMROCK_VERSION_VARIABLE);
        final Xpp3Dom executions = new Xpp3Dom("executions");
        final Xpp3Dom execution = new Xpp3Dom("execution");
        final Xpp3Dom goals = new Xpp3Dom("goals");
        final Xpp3Dom goal = new Xpp3Dom("goal");
        goal.setValue("build");
        goals.addChild(goal);
        execution.addChild(goals);
        executions.addChild(execution);

        shamrockMavenPlugin.setConfiguration(executions);
        build.addPlugin(shamrockMavenPlugin);

        model.setBuild(build);
    }

    private void setProperties(Model model) {
        Properties properties = new Properties();
        properties.put(SHAMROCK_VERSION_PROPERTY_NAME, SHAMROCK_VERSION);
        properties.put("shamrock-maven-plugin.version", SHAMROCK_VERSION);
    }



    private void addRuntimeProfile(Model model) {
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


        List<Profile> profiles = model.getProfiles();
        profiles.add(profile);
        model.setProfiles(profiles);
    }

    private Plugin createNativePlugin() {
        Plugin plugin = new Plugin();
        plugin.setGroupId("org.jboss.shamrock");
        plugin.setArtifactId("shamrock-maven-plugin");
        plugin.setVersion(SHAMROCK_VERSION_VARIABLE);

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
        Xpp3Dom config = null;
        try {
            config = Xpp3DomBuilder.build(new StringReader(configString));
        }
        catch(XmlPullParserException  | IOException e) {
            e.printStackTrace();
        }

        return config;
    }

    private void addShamrockDependencies(Model model) {
        List<Dependency> dependencyList = new ArrayList<>();

        Dependency jaxrs = new Dependency();
        jaxrs.setGroupId("org.jboss.shamrock");
        jaxrs.setArtifactId("shamrock-jaxrs-deployment");
        jaxrs.setVersion(SHAMROCK_VERSION_VARIABLE);
        dependencyList.add(jaxrs);

        Dependency arc = new Dependency();
        arc.setGroupId("org.jboss.shamrock");
        arc.setArtifactId("shamrock-arc-deployment");
        arc.setVersion(SHAMROCK_VERSION_VARIABLE);
        dependencyList.add(arc);

        /*
        Dependency logging = new Dependency();
        logging.setGroupId("org.jboss.shamrock");
        logging.setArtifactId("shamrock-logging-deployment");
        logging.setVersion(SHAMROCK_VERSION);
        dependencyList.add(logging);
        */

        model.setDependencies(dependencyList);
    }

    private void setGroupArtifactAndVersion(Model model) {
        model.setGroupId(groupId);
        model.setArtifactId(artifactId);
        model.setVersion(version);
    }


}
