package org.jboss.shamrock.cli.commands;


import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
public class CreateProject {

    private File root;
    private final String groupId;
    private final String artifactId;
    private final String version;

    private static final String SHAMROCK_GROUP_ID = "org.jboss.shamrock";
    private static final String SHAMROCK_VERSION = "1.0.0.Alpha1-SNAPSHOT";
    private static final String SHAMROCK_VERSION_PROPERTY_NAME = "shamrock.version";
    private static final String SHAMROCK_VERSION_VARIABLE = "${"+ SHAMROCK_VERSION_PROPERTY_NAME +"}";

    public CreateProject(final File file, String groupId, String artifactId, String version) {
        root = file;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    public boolean doCreateProject() {
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

        File pom = new File(root + "/pom.xml");

        try {
            writeProjectFiles();
            createPom(pom);
        }
        catch(IOException | XmlPullParserException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private void writeProjectFiles() throws IOException {
        //  Note:  this current approach is terrible.  I know.  I want to get everything else working and then figure out a nicer way of
        //  doing discovery of template resources.

        final InputStream stream = getClass().getResourceAsStream("/template-contents");
        try(final BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while((line = reader.readLine()) != null) {
                final String name = "./basic-rest/";
                if(line.startsWith(name)) {
                    writeResource(name, line);
                }
            }
        }
    }

    private void writeResource(final String name, final String path) throws IOException {
        final File outputFile = new File(root, path.replace(name, ""));
        outputFile.getParentFile().mkdirs();
        try(final InputStream resource = getClass().getResourceAsStream("/templates/" + path)) {
            java.nio.file.Files.copy(resource, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void createPom(File pom) throws IOException, XmlPullParserException {

        final Model model;
        try(final FileReader fileReader = new FileReader(pom)) {
            model = new MavenXpp3Reader().read(fileReader);
        }

        setGroupArtifactAndVersion(model);
        setProperties(model);
        addMavenCompilerPlugin(model);
        updateShamrockDependencies(model);

        try(Writer writer = new FileWriter(pom)) {
            new MavenXpp3Writer().write(writer, model);
        }
    }

    private void addMavenCompilerPlugin(Model model) {
        Build build = new Build();

        Plugin mavenCompilerPlugin = new Plugin();
        mavenCompilerPlugin.setGroupId("org.apache.maven.plugins");
        mavenCompilerPlugin.setArtifactId("maven-compiler-plugin");
        mavenCompilerPlugin.setVersion("3.8.0");

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
        model.setProperties(properties);
    }

    private void updateShamrockDependencies(Model model) {
        model.getDependencies().stream()
             .filter(d -> d.getGroupId().equals(SHAMROCK_GROUP_ID))
             .forEach(d -> d.setVersion(SHAMROCK_VERSION_VARIABLE));

        model.getBuild().getPlugins().stream()
             .filter(d -> d.getGroupId().equals(SHAMROCK_GROUP_ID))
             .forEach(d -> d.setVersion(SHAMROCK_VERSION_VARIABLE));
    }

    private void setGroupArtifactAndVersion(Model model) {
        model.setGroupId(groupId);
        model.setArtifactId(artifactId);
        model.setVersion(version);
    }
}
