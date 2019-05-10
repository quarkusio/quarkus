package io.quarkus.maven;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AddExtensionMojoTest {

    private static final File MIN_POM = new File("src/test/resources/projects/simple-pom-it/pom.xml");
    private static final File OUTPUT_POM = new File("target/test-classes/pom.xml");
    private static final String DEP_GAV = "org.apache.commons:commons-lang3:3.8.1";
    private AddExtensionMojo mojo;

    @BeforeEach
    void init() throws IOException {
        mojo = getMojo();
        mojo.project = new MavenProject();
        Model model = new Model();
        mojo.project.setModel(model);
        mojo.project.setOriginalModel(model);
        FileUtils.copyFile(MIN_POM, OUTPUT_POM);
        model.setPomFile(OUTPUT_POM);
    }

    protected AddExtensionMojo getMojo() {
        return new AddExtensionMojo();
    }

    @Test
    void testAddSingleDependency() throws MojoExecutionException, IOException, XmlPullParserException {
        mojo.extension = DEP_GAV;
        mojo.extensions = new HashSet<>();
        mojo.execute();

        Model reloaded = reload();
        List<Dependency> dependencies = reloaded.getDependencies();
        assertThat(dependencies).hasSize(1);
        assertThat(dependencies.get(0).getArtifactId()).isEqualTo("commons-lang3");
    }

    @Test
    void testAddMultipleDependency() throws MojoExecutionException, IOException, XmlPullParserException {
        Set<String> deps = new HashSet<>();
        deps.add(DEP_GAV);
        deps.add("commons-io:commons-io:2.6");
        mojo.extensions = deps;
        mojo.execute();

        Model reloaded = reload();
        List<Dependency> dependencies = reloaded.getDependencies();
        assertThat(dependencies).hasSize(2);
    }

    @Test
    void testThatBothParameterCannotBeSet() {
        mojo.extension = DEP_GAV;
        Set<String> deps = new HashSet<>();
        deps.add("commons-io:commons-io:2.6");
        mojo.extensions = deps;

        assertThrows(MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void testThatAtLeastOneParameterMustBeSet() {
        assertThrows(MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void testThatAtLeastOneParameterMustBeSetWithBlankAndEmpty() {
        mojo.extension = "";
        mojo.extensions = Collections.emptySet();
        assertThrows(MojoExecutionException.class, () -> mojo.execute());
    }

    private Model reload() throws IOException, XmlPullParserException {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        try (Reader fr = Files.newBufferedReader(OUTPUT_POM.toPath())) {
            return reader.read(fr);
        }
    }

}
