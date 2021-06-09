package io.quarkus.maven;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.*;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.workspace.ModelUtils;
import io.quarkus.devtools.testing.RegistryClientTestHelper;

class AddExtensionMojoTest {

    private static final File MIN_POM = new File("target/test-classes/projects/simple-pom-it/pom.xml");
    private static final File OUTPUT_POM = new File("target/test-classes/add-extension/pom.xml");
    private static final String DEP_GAV = "org.apache.commons:commons-lang3:3.8.1";
    private AddExtensionMojo mojo;

    @BeforeAll
    static void globalInit() {
        RegistryClientTestHelper.enableRegistryClientTestConfig();
    }

    @AfterAll
    static void globalCleanUp() {
        RegistryClientTestHelper.disableRegistryClientTestConfig();
    }

    @BeforeEach
    void init() throws Exception {
        mojo = getMojo();
        mojo.project = new MavenProject();
        mojo.project.setPomFile(OUTPUT_POM);
        mojo.project.setFile(OUTPUT_POM);
        FileUtils.copyFile(MIN_POM, OUTPUT_POM);

        Model model = ModelUtils.readModel(OUTPUT_POM.toPath());
        model.setPomFile(OUTPUT_POM);
        mojo.project.setOriginalModel(model);

        final MavenArtifactResolver mvn = new MavenArtifactResolver(
                new BootstrapMavenContext(BootstrapMavenContext.config()
                        .setCurrentProject(OUTPUT_POM.getAbsolutePath())
                        .setOffline(true)));
        mojo.repoSystem = mvn.getSystem();
        mojo.repoSession = mvn.getSession();
        mojo.repos = mvn.getRepositories();
        mojo.remoteRepositoryManager = mvn.getRemoteRepositoryManager();

        final Model effectiveModel = model.clone();
        final DependencyManagement dm = new DependencyManagement();
        effectiveModel.setDependencyManagement(dm);
        final Artifact projectPom = new DefaultArtifact(ModelUtils.getGroupId(model), model.getArtifactId(), null, "pom",
                ModelUtils.getVersion(model));
        final ArtifactDescriptorResult descriptor = mvn.resolveDescriptor(projectPom);
        descriptor.getManagedDependencies().forEach(d -> {
            final Dependency dep = new Dependency();
            Artifact a = d.getArtifact();
            dep.setGroupId(a.getGroupId());
            dep.setArtifactId(a.getArtifactId());
            dep.setClassifier(a.getClassifier());
            dep.setType(a.getExtension());
            dep.setVersion(a.getVersion());
            if (d.getOptional() != null) {
                dep.setOptional(d.getOptional());
            }
            dep.setScope(d.getScope());
            dm.addDependency(dep);
        });
        descriptor.getDependencies().forEach(d -> {
            final Dependency dep = new Dependency();
            Artifact a = d.getArtifact();
            dep.setGroupId(a.getGroupId());
            dep.setArtifactId(a.getArtifactId());
            dep.setClassifier(a.getClassifier());
            dep.setType(a.getExtension());
            dep.setVersion(a.getVersion());
            if (d.getOptional() != null) {
                dep.setOptional(d.getOptional());
            }
            dep.setScope(d.getScope());
            effectiveModel.addDependency(dep);
        });
        descriptor.getProperties().entrySet().forEach(p -> effectiveModel.getProperties().setProperty(p.getKey(),
                p.getValue() == null ? "" : p.getValue().toString()));
        mojo.project.setModel(effectiveModel);
    }

    protected AddExtensionMojo getMojo() throws Exception {
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
