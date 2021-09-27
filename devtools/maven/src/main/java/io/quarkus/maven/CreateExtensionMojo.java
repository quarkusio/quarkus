package io.quarkus.maven;

import static io.quarkus.devtools.commands.CreateExtension.extractQuarkiverseExtensionId;
import static io.quarkus.devtools.commands.CreateExtension.isQuarkiverseGroupId;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.fusesource.jansi.Ansi.ansi;

import java.io.File;
import java.io.IOException;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.jboss.logging.Logger;

import io.quarkus.devtools.commands.CreateExtension;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.maven.components.Prompter;

/**
 * Creates the base of a
 * <a href="https://quarkus.io/guides/writing-extensions">Quarkus Extension</a> in different layout depending of the options and
 * environment.
 * <br />
 * <br />
 * <h2>Create in the quarkus-parent project directory (or the extensions parent dir)</h2>
 * <br />
 * It will:
 * <ul>
 * <li>generate the new Quarkus extension in the extensions parent as a module (parent, runtime and deployment), with unit test
 * and devmode test on option.</li>
 * <li>On option, generate the new integration test in the integration tests parent as a module.</li>
 * <li>add the dependencies to the bom/application/pom.xml.</li>
 * </ul>
 * <br />
 * <h2>Creating a Quarkiverse extension</h2>
 * <br />
 * When using <code>-DgroupId=io.quarkiverse.[featureId]</code>, the new extension will use the Quarkiverse layout.
 * <br />
 * <br />
 * <h2>Creating a standalone extension</h2>
 * <br />
 * <ul>
 * <li>generate the new Quarkus extension in the current directory (parent, runtime and deployment), with unit test and devmode
 * test on option.</li>
 * <li>On option, generate the new integration test module in the current directory.</li>
 * </ul>
 */
@Mojo(name = "create-extension", requiresProject = false)
public class CreateExtensionMojo extends AbstractMojo {

    private static final Logger log = Logger.getLogger(CreateExtensionMojo.class);

    /**
     * Directory where the changes should be performed.
     * <br />
     * <br />
     * Default: the current directory of the current Java process.
     */
    @Parameter(property = "basedir")
    File basedir;

    /**
     * {@code extensionId} of this extension (REQUIRED).
     * <br />
     * <br />
     * It will be used to generate the different extension modules artifactIds
     * (<code>[namespaceId][extensionId]-parent</code>), runtime (<code>[namespaceId][extensionId]</code>) and deployment
     * (<code>[namespaceId][extensionId]-deployment</code>).
     */
    @Parameter(property = "extensionId")
    String extensionId;

    /**
     * The {@code groupId} for the newly created Maven modules (REQUIRED - INHERITED IN QUARKUS-CORE).
     */
    @Parameter(property = "groupId")
    String groupId;

    /**
     * Quarkus version the newly created extension should depend on (REQUIRED - INHERITED IN QUARKUS-CORE).
     */
    @Parameter(property = "quarkusVersion")
    String quarkusVersion;

    /**
     * A prefix common to all extension artifactIds in the current source tree.
     * <br />
     * <br />
     * Default: "quarkus-" in quarkus Quarkus Core and Quarkiverse else ""
     */
    @Parameter(property = "namespaceId")
    String namespaceId;

    /**
     * The {@code version} for the newly created Maven modules.
     * <br />
     * <br />
     * Default: automatic in Quarkus Core else {@link CreateExtension#DEFAULT_VERSION}
     */
    @Parameter(property = "version")
    String version;

    /**
     * The {@code extensionName} of the runtime module. The {@code extensionName}s of the extension parent and deployment
     * modules will be
     * based on this {@code name} too.
     * <br />
     * <br />
     * Default: the formatted {@code extensionId}
     */
    @Parameter(property = "extensionName")
    String extensionName;

    /**
     * A prefix common to all extension names in the current source tree.
     * <br />
     * <br />
     * Default: "quarkus-" in quarkus Quarkus Core and Quarkiverse else ""
     */
    @Parameter(property = "namespaceName")
    String namespaceName;

    /**
     * Base package under which classes should be created in Runtime and Deployment modules.
     * <br />
     * <br />
     * Default: auto-generated out of {@link #groupId}, {@link #namespaceId} and {@link #extensionId}
     */
    @Parameter(property = "packageName")
    String packageName;

    /**
     * The {@code groupId} of the Quarkus platform BOM.
     * <br />
     * <br />
     * Default: {@link CreateExtension#DEFAULT_BOM_GROUP_ID}
     */
    @Parameter(property = "quarkusBomGroupId")
    String quarkusBomGroupId;

    /**
     * The {@code artifactId} of the Quarkus platform BOM.
     * <br />
     * <br />
     * Default: {@link CreateExtension#DEFAULT_BOM_ARTIFACT_ID}
     */
    @Parameter(property = "quarkusBomArtifactId")
    String quarkusBomArtifactId;

    /**
     * The {@code version} of the Quarkus platform BOM.
     * <br />
     * <br />
     * Default: {@link CreateExtension#DEFAULT_BOM_VERSION}
     */
    @Parameter(property = "quarkusBomVersion")
    String quarkusBomVersion;

    /**
     * Indicates whether to generate a unit test class for the extension
     */
    @Parameter(property = "withoutUnitTest")
    boolean withoutUnitTest;

    /**
     * Indicates whether to generate an integration tests for the extension
     */
    @Parameter(property = "withoutIntegrationTests")
    boolean withoutIntegrationTests;

    /**
     * Indicates whether to generate a devmode test for the extension
     */
    @Parameter(property = "withoutDevModeTest")
    boolean withoutDevModeTest;

    /**
     * Indicates whether to generate any tests for the extension (same as
     * <code>-DwithoutUnitTest -DwithoutIntegrationTest -DwithoutDevModeTest</code>)
     */
    @Parameter(property = "withoutTests")
    boolean withoutTests;

    /**
     * Used to detect legacy command usage and display an error
     */
    @Parameter(property = "artifactId")
    String artifactId;

    @Parameter(defaultValue = "${project}")
    protected MavenProject project;

    @Parameter(defaultValue = "${session}")
    private MavenSession session;

    @Override
    public void execute() throws MojoExecutionException {

        if (!isBlank(artifactId)) {
            if (isBlank(extensionId)) {
                getLog().warn(ansi().a(
                        "the given 'artifactId' has been automatically converted to 'extensionId' for backward compatibility.")
                        .toString());
                if (artifactId.startsWith("quarkus-")) {
                    namespaceId = "quarkus-";
                    extensionId = artifactId.replace("quarkus-", "");
                } else {
                    extensionId = artifactId;
                }
            }
        }

        promptValues();

        autoComputeQuarkiverseExtensionId();

        final CreateExtension createExtension = new CreateExtension(basedir.toPath())
                .extensionId(extensionId)
                .extensionName(extensionName)
                .groupId(groupId)
                .version(version)
                .packageName(packageName)
                .namespaceId(namespaceId)
                .namespaceName(namespaceName)
                .quarkusVersion(quarkusVersion)
                .quarkusBomGroupId(quarkusBomGroupId)
                .quarkusBomArtifactId(quarkusBomArtifactId)
                .quarkusBomGroupId(quarkusBomVersion)
                .withoutUnitTest(withoutTests || withoutUnitTest)
                .withoutDevModeTest(withoutTests || withoutDevModeTest)
                .withoutIntegrationTests(withoutTests || withoutIntegrationTests);

        boolean success;

        try {
            success = createExtension.execute().isSuccess();
        } catch (QuarkusCommandException e) {
            throw new MojoExecutionException("Failed to generate Extension", e);
        }

        if (!success) {
            throw new MojoExecutionException("Failed to generate Extension");
        }
    }

    private void autoComputeQuarkiverseExtensionId() {
        if (isQuarkiverseGroupId(groupId) && isEmpty(extensionId)) {
            extensionId = extractQuarkiverseExtensionId(groupId);
        }
    }

    private String getPluginVersion() throws MojoExecutionException {
        return CreateUtils.resolvePluginInfo(CreateExtensionLegacyMojo.class).getVersion();
    }

    private void promptValues() throws MojoExecutionException {
        if (!session.getRequest().isInteractiveMode()) {
            return;
        }
        try {
            final Prompter prompter = new Prompter();
            if (project == null || !project.getArtifactId().endsWith("quarkus-parent")) {
                if (isBlank(quarkusVersion)) {
                    quarkusVersion = getPluginVersion();
                }
                if (isBlank(groupId)) {
                    prompter.addPrompt("Set the extension groupId: ", "org.acme", input -> groupId = input);
                }
            }
            autoComputeQuarkiverseExtensionId();
            if (isBlank(extensionId)) {
                prompter.addPrompt("Set the extension id: ", input -> extensionId = input);
            }
            prompter.collectInput();
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to get user input", e);
        }
    }
}
