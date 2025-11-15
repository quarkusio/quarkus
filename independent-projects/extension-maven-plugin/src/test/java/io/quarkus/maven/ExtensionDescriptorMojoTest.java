package io.quarkus.maven;

import static io.quarkus.maven.ExtensionDescriptorMojo.getCodestartArtifact;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith(SystemStubsExtension.class)
class ExtensionDescriptorMojoTest extends AbstractMojoTestCase {

    @SystemStub
    private EnvironmentVariables environment;

    private static final boolean RESOLVE_OFFLINE = true;
    // Test resources end up in target/test-classes after filtering
    public static final String TEST_RESOURCES = "target/test-classes/";

    @BeforeEach
    public void setup() throws Exception {
        super.setUp();
        // Make sure that we don't have the GITHUB_REPOSITORY environment variable masking what this mojo does
        environment.set("GITHUB_REPOSITORY", null);
    }

    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();

        // Assume that all our test data has a common org, for ease of cleanup
        File repoPath = new File(getLocalRepoPath(), "io/quackiverse");
        deleteDirectory(repoPath);
    }

    @Test
    public void shouldExecuteSimplePomCleanly()
            throws Exception {
        ExtensionDescriptorMojo mojo = makeMojo("simple-pom-with-checks-disabled");
        mojo.execute();
    }

    @Test
    public void shouldCreateExtensionProperties()
            throws Exception {
        ExtensionDescriptorMojo mojo = makeMojo("simple-pom-with-checks-disabled");
        File propertiesFile = getGeneratedExtensionMetadataFile(mojo.project.getBasedir(),
                "target/classes/META-INF/quarkus-extension.properties");

        // Tidy up any artifacts from previous runs
        if (propertiesFile.exists()) {
            Files.delete(propertiesFile.toPath());
        }
        mojo.execute();
        assertTrue(propertiesFile.exists());
    }

    @Test
    public void shouldCreateMetadata()
            throws Exception {

        ExtensionDescriptorMojo mojo = makeMojo("simple-pom-with-checks-disabled");
        File yamlFile = getGeneratedExtensionMetadataFile(mojo.project.getBasedir(),
                "target/classes/META-INF/quarkus-extension.yaml");

        // Tidy up any artifacts from previous runs
        if (yamlFile.exists()) {
            Files.delete(yamlFile.toPath());
        }
        mojo.execute();
        assertTrue(yamlFile.exists());

        String fileContents = readFileAsString(yamlFile);
        assertYamlContains(fileContents, "name", "an arbitrary name");
        assertYamlContains(fileContents, "artifact", "io.quackiverse:test-artifact::jar:1.4.2-SNAPSHOT");

        assertYamlContains(fileContents, "scm-url", "https://github.com/from/pom");

    }

    @Test
    public void shouldReadLocalParentsForScmInfo()
            throws Exception {

        ExtensionDescriptorMojo mojo = makeMojo("simple-pom-with-checks-disabled-and-local-parent/child");
        File yamlFile = getGeneratedExtensionMetadataFile(mojo.project.getBasedir(),
                "target/classes/META-INF/quarkus-extension.yaml");

        // Tidy up any artifacts from previous runs
        if (yamlFile.exists()) {
            Files.delete(yamlFile.toPath());
        }
        mojo.execute();
        assertTrue(yamlFile.exists());

        String fileContents = readFileAsString(yamlFile);
        assertYamlContains(fileContents, "artifact", "io.quackiverse:test-artifact-child::jar:1.4.2-SNAPSHOT");

        assertYamlContains(fileContents, "scm-url", "https://github.com/from/parent");

    }

    @Test
    public void shouldProcessRealisticExtensionCleanly()
            throws Exception {

        // We need to get these poms into our local repository before executing the plugin
        mavenExecPom("fake-extension-runtime");
        mavenExecPom("fake-extension-deployment");

        // The test extension, by design, resolves dependencies of an older (and outside the main project) Quarkus version
        // Usually it builds fine, even offline, but we cannot count on everything being already downloaded, especially
        ExtensionDescriptorMojo mojo = makeMojo("fake-extension-runtime", false);
        mojo.execute();

    }

    @Test
    public void shouldFlagMissingDependenciesInARealisticExtension()
            throws Exception {

        // Build both halves of the extension first so the install phase is done
        mavenExecPom("fake-extension-runtime-with-missing-dependencies");
        mavenExecPom("fake-extension-deployment");

        Exception thrown = Assertions.assertThrows(MojoExecutionException.class, () -> {
            ExtensionDescriptorMojo mojo = makeMojo("fake-extension-runtime-with-missing-dependencies", false);
            mojo.execute();
        });
        assertTrue("Message format does not match expectations: \n" + thrown.getMessage(),
                thrown.getMessage().contains(" corresponding runtime artifacts were not found"));
        assertTrue("Missing artifact 'io.quarkus:quarkus-arc-deployment::jar' is not flagged: \n" + thrown.getMessage(),
                thrown.getMessage().contains("io.quarkus:quarkus-arc-deployment::jar"));
        assertTrue(
                "Missing artifact 'io.quarkus:quarkus-smallrye-context-propagation-deployment::jar' is not flagged: \n"
                        + thrown.getMessage(),
                thrown.getMessage().contains("io.quarkus:quarkus-smallrye-context-propagation-deployment::jar"));
    }

    @Test
    public void shouldFlagIncorrectRuntimeDependencyOnDeployment()
            throws Exception {

        // Build both halves of the extension first so the install phase is done
        mavenExecPom("fake-extension-runtime-with-deployment-dependency");
        mavenExecPom("fake-extension-deployment");

        Exception thrown = Assertions.assertThrows(MojoExecutionException.class, () -> {
            ExtensionDescriptorMojo mojo = makeMojo("fake-extension-runtime-with-missing-dependencies", false);
            mojo.execute();
        });
        String message = thrown.getMessage();
        assertTrue("Message format does not match expectations: \n" + message,
                message.contains("depends on the following Quarkus extension deployment artifacts")
                        || message.contains("The following deployment artifact(s) appear on the runtime classpath"));
    }

    // TODO we could also add some more tests about the dependency resolution and other extension descriptor functions here; see ExtensionDescriptorTaskTest for some ideas

    private File getGeneratedExtensionMetadataFile(File project, String child) {
        return new File(project, child);
    }

    private void assertYamlContains(File file, String key, String value) throws IOException {
        assertYamlContains(readFileAsString(file), key, value);
    }

    // Naive yaml processing; we do it this way for readability, and simplicity, and because it's how a human would check it by eye.
    // If we wanted to be less naive, we could do `ObjectNode extensionDescriptor = TestUtils.readExtensionFile`
    private void assertYamlContains(String fileContents, String key, String value) {
        // ... we should probably cache the file read
        assertTrue("Missing key '" + key + "' in \n" + fileContents, fileContents.contains(key));
        assertTrue("Missing value '" + value + "' in \n" + fileContents, fileContents.contains(key + ": \"" + value + "\""));

    }

    // Naive yaml processing; we do it this way for readability, and simplicity, and because it's how a human would check it by eye.
    // If we wanted to be less naive, we could do `ObjectNode extensionDescriptor = TestUtils.readExtensionFile`
    private void assertYamlContainsObject(String fileContents, String key) {
        // ... we should probably cache the file read
        assertTrue("Missing key '" + key + "' in \n" + fileContents, fileContents.contains(key));
        assertTrue("No children? in \n" + fileContents, fileContents.contains(key + ":\n"));

    }

    private static String readFileAsString(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()));
    }

    @Test
    public void testGetCodestartArtifact() {
        assertEquals("io.quarkus:my-ext:999-SN",
                getCodestartArtifact("io.quarkus:my-ext", "999-SN"));
        assertEquals("io.quarkus:my-ext:codestarts:jar:999-SN",
                getCodestartArtifact("io.quarkus:my-ext:codestarts:jar:${project.version}", "999-SN"));
        assertEquals("io.quarkus:my-ext:codestarts:jar:1.0",
                getCodestartArtifact("io.quarkus:my-ext:codestarts:jar:1.0", "999-SN"));
        assertEquals("io.quarkus:my-ext:999-SN",
                getCodestartArtifact("io.quarkus:my-ext:${project.version}", "999-SN"));
    }

    private ExtensionDescriptorMojo makeMojo(String dirName) throws Exception {
        return makeMojo(dirName, RESOLVE_OFFLINE);
    }

    private ExtensionDescriptorMojo makeMojo(String dirName, boolean resolveOffline) throws Exception {
        File basedir = getTestFile(TEST_RESOURCES + dirName);
        File pom = new File(basedir, "pom.xml");

        MavenProject project = readMavenProject(basedir);
        MavenSession session = newMavenSession(project);

        // add localRepo - framework doesn't do this on its own
        ArtifactRepository localRepo = createLocalArtifactRepository();
        session.getRequest().setLocalRepository(localRepo);
        session.getRequest().setBaseDirectory(basedir);

        final MavenArtifactResolver mvn = new MavenArtifactResolver(
                new BootstrapMavenContext(BootstrapMavenContext.config()
                        .setCurrentProject(pom.getAbsolutePath())
                        .setOffline(resolveOffline)));

        ExtensionDescriptorMojo mojo = (ExtensionDescriptorMojo) lookupConfiguredMojo(session,
                newMojoExecution("extension-descriptor"));
        mojo.repoSystem = mvn.getSystem();
        mojo.repoSession = mvn.getSession();
        mojo.workspaceProvider = BootstrapWorkspaceProvider.newInstance(basedir.getAbsolutePath());
        return mojo;
    }

    protected MavenProject readMavenProject(File basedir)
            throws ProjectBuildingException, ComponentLookupException {
        File pom = getGeneratedExtensionMetadataFile(basedir, "pom.xml");
        assertTrue(pom.exists());

        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setBaseDirectory(basedir);
        ProjectBuildingRequest configuration = request.getProjectBuildingRequest();
        configuration.setRepositorySession(new DefaultRepositorySystemSession());
        configuration.setLocalRepository(createLocalArtifactRepository());
        MavenProject project = lookup(ProjectBuilder.class).build(pom, configuration).getProject();
        assertNotNull(project);
        return project;
    }

    private ArtifactRepository createLocalArtifactRepository() {
        String repo = getLocalRepoPath();
        File localRepoDir = new File(repo);
        return new MavenArtifactRepository("local",
                localRepoDir.toURI().toString(),
                new DefaultRepositoryLayout(),
                new ArtifactRepositoryPolicy(true, ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS,
                        ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE),
                new ArtifactRepositoryPolicy(true, ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS,
                        ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE)

        );
    }

    private String getLocalRepoPath() {
        String repo = System.getProperty("maven.repo.local");
        if (repo == null) {
            repo = new File(System.getProperty("user.home"), ".m2/repository").getAbsolutePath();
        }
        return repo;
    }

    private void mavenExecPom(String dirName) throws MavenInvocationException {
        File basedir = getTestFile(TEST_RESOURCES + dirName);
        File pom = new File(basedir, "pom.xml");

        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(pom);
        request.setGoals(Collections.singletonList("install"));
        request.setDebug(false);
        request.setShowErrors(true);
        request.setBatchMode(true);

        Invoker invoker = new DefaultInvoker();
        invoker.setMavenHome(new File(basedir, "../"));
        invoker.setMavenExecutable(new File(basedir, "../../mvnw").getAbsoluteFile());

        invoker.execute(request);
    }

    private void deleteDirectory(File fileToDelete) throws IOException {

        if (fileToDelete.exists()) {
            Path pathToBeDeleted = fileToDelete.toPath();

            Files.walk(pathToBeDeleted)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);

            assertFalse("Directory still exists",
                    Files.exists(pathToBeDeleted));
        }
    }
}
