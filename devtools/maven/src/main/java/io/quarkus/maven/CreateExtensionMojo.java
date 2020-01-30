package io.quarkus.maven;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.lang.model.SourceVersion;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.jboss.logging.Logger;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.MavenRepoInitializer;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalWorkspace;
import io.quarkus.maven.utilities.MojoUtils;
import io.quarkus.maven.utilities.PomTransformer;
import io.quarkus.maven.utilities.PomTransformer.Gavtcs;
import io.quarkus.maven.utilities.PomTransformer.Transformation;

/**
 * Creates a triple of stub Maven modules (Parent, Runtime and Deployment) to implement a new
 * <a href="https://quarkus.io/guides/extension-authors-guide">Quarkus Extension</a>.
 *
 * <h2>Adding into an established source tree</h2>
 * <p>
 * If this Mojo is executed in a directory that contains a {@code pom.xml} file with packaging {@code pom} the newly
 * created Parent module is added as a child module to the existing {@code pom.xml} file.
 *
 * <h2>Creating a source tree from scratch</h2>
 * <p>
 * Executing this Mojo in an empty directory is not supported yet.
 */
@Mojo(name = "create-extension", requiresProject = false)
public class CreateExtensionMojo extends AbstractMojo {

    private static final String QUOTED_DOLLAR = Matcher.quoteReplacement("$");

    private static final Logger log = Logger.getLogger(CreateExtensionMojo.class);

    private static final Pattern BRACKETS_PATTERN = Pattern.compile("[()]+");
    private static final String CLASSPATH_PREFIX = "classpath:";
    private static final String FILE_PREFIX = "file:";

    private static final String QUARKUS_VERSION_PROP = "quarkus.version";

    static final String DEFAULT_ENCODING = "utf-8";
    static final String DEFAULT_QUARKUS_VERSION = "@{" + QUARKUS_VERSION_PROP + "}";
    static final String QUARKUS_VERSION_POM_EXPR = "${" + QUARKUS_VERSION_PROP + "}";
    static final String DEFAULT_BOM_ENTRY_VERSION = "@{project.version}";
    static final String DEFAULT_TEMPLATES_URI_BASE = "classpath:/create-extension-templates";
    static final String DEFAULT_NAME_SEGMENT_DELIMITER = " - ";
    static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("@\\{([^\\}]+)\\}");

    static final String PLATFORM_DEFAULT_GROUP_ID = "io.quarkus";
    static final String PLATFORM_DEFAULT_ARTIFACT_ID = "quarkus-bom-deployment";

    static final String COMPILER_PLUGIN_VERSION_PROP = "compiler-plugin.version";
    static final String COMPILER_PLUGIN_VERSION_POM_EXPR = "${" + COMPILER_PLUGIN_VERSION_PROP + "}";
    static final String COMPILER_PLUGIN_DEFAULT_VERSION = "3.8.1";
    private static final String COMPILER_PLUGIN_GROUP_ID = "org.apache.maven.plugins";
    private static final String COMPILER_PLUGIN_ARTIFACT_ID = "maven-compiler-plugin";
    private static final String COMPILER_PLUGIN_KEY = COMPILER_PLUGIN_GROUP_ID + ":"
            + COMPILER_PLUGIN_ARTIFACT_ID;

    /**
     * Directory where the changes should be performed. Default is the current directory of the current Java process.
     *
     * @since 0.20.0
     */
    @Parameter(property = "quarkus.basedir")
    File basedir;

    /**
     * The {@code groupId} of the Quarkus platform's BOM containing deployment dependencies.
     */
    @Parameter(property = "platformGroupId", defaultValue = PLATFORM_DEFAULT_GROUP_ID)
    String platformGroupId;

    /**
     * The {@code artifactId} of the Quarkus platform's BOM containing deployment dependencies.
     */
    @Parameter(property = "platformArtifactId", defaultValue = PLATFORM_DEFAULT_ARTIFACT_ID)
    String platformArtifactId;

    /**
     * The {@code groupId} for the newly created Maven modules. If {@code groupId} is left unset, the {@code groupId}
     * from the {@code pom.xml} in the current directory will be used. Otherwise, an exception is thrown.
     *
     * @since 0.20.0
     */
    @Parameter(property = "groupId")
    String groupId;

    /**
     * This parameter was introduced to change the property of {@code groupId} parameter from {@code quarkus.groupId} to
     * {@code groupId}
     *
     * @since 1.3.0
     */
    @Deprecated
    @Parameter(property = "quarkus.groupId")
    String deprecatedGroupId;

    /**
     * {@code artifactId} of the runtime module. The {@code artifactId}s of the extension parent
     * (<code>${artifactId}-parent</code>) and deployment (<code>${artifactId}-deployment</code>) modules will be based
     * on this {@code artifactId} too.
     * <p>
     * Optionally, this value can contain the {@link #artifactIdBase} enclosed in round brackets, e.g.
     * {@code my-project-(cool-extension)}, where {@code cool-extension} is an {@link #artifactIdBase} and
     * {@code my-project-} is {@link #artifactIdPrefix}. This is a way to avoid defining of {@link #artifactIdPrefix}
     * and {@link #artifactIdBase} separately. If no round brackets are present in {@link #artifactId},
     * {@link #artifactIdBase} will be equal to {@link #artifactId} and {@link #artifactIdPrefix} will be an empty
     * string.
     *
     * @since 0.20.0
     */
    @Parameter(property = "artifactId")
    String artifactId;

    /**
     * This parameter was introduced to change the property of {@code artifactId} parameter from {@code quarkus.artifactId} to
     * {@code artifactId}
     *
     * @since 1.3.0
     */
    @Deprecated
    @Parameter(property = "quarkus.artifactId")
    String deprecatedArtifactId;

    /**
     * A prefix common to all extension artifactIds in the current source tree. If you set {@link #artifactIdPrefix},
     * set also {@link #artifactIdBase}, but do not set {@link #artifactId}.
     *
     * @since 0.20.0
     */
    @Parameter(property = "quarkus.artifactIdPrefix", defaultValue = "")
    String artifactIdPrefix;

    /**
     * The unique part of the {@link #artifactId}. If you set {@link #artifactIdBase}, {@link #artifactIdPrefix}
     * may also be set, but not {@link #artifactId}.
     *
     * @since 0.20.0
     */
    @Parameter(property = "quarkus.artifactIdBase")
    String artifactIdBase;

    /**
     * The {@code version} for the newly created Maven modules. If {@code version} is left unset, the {@code version}
     * from the {@code pom.xml} in the current directory will be used. Otherwise, an exception is thrown.
     *
     * @since 0.20.0
     */
    @Parameter(property = "version")
    String version;

    /**
     * This parameter was introduced to change the property of {@code version} parameter from {@code quarkus.artifactVersion} to
     * {@code version}
     *
     * @since 1.3.0
     */
    @Deprecated
    @Parameter(property = "quarkus.artifactVersion")
    String deprecatedVersion;

    /**
     * The {@code name} of the runtime module. The {@code name}s of the extension parent and deployment modules will be
     * based on this {@code name} too.
     * <p>
     * Optionally, this value can contain the {@link #nameBase} enclosed in round brackets, e.g.
     * {@code My Project - (Cool Extension)}, where {@code Cool Extension} is a {@link #nameBase} and
     * {@code My Project - } is {@link #namePrefix}. This is a way to avoid defining of {@link #namePrefix} and
     * {@link #nameBase} separately. If no round brackets are present in {@link #name}, the {@link #nameBase} will be
     * equal to {@link #name} and {@link #namePrefix} will be an empty string.
     *
     * @since 0.20.0
     */
    @Parameter(property = "quarkus.name")
    String name;

    /**
     * A prefix common to all extension names in the current source tree. If you set {@link #namePrefix}, set also
     * {@link #nameBase}, but do not set {@link #name}.
     *
     * @since 0.20.0
     */
    @Parameter(property = "quarkus.namePrefix")
    String namePrefix;

    /**
     * The unique part of the {@link #name}. If you set {@link #nameBase}, set also {@link #namePrefix}, but do not set
     * {@link #name}.
     * <p>
     * If neither {@link #name} nor @{link #nameBase} is set, @{link #nameBase} will be derived from
     * {@link #artifactIdBase}.
     *
     * @since 0.20.0
     */
    @Parameter(property = "quarkus.nameBase")
    String nameBase;

    /**
     * A string that will delimit {@link #name} from {@code Parent}, {@code Runtime} and {@code Deployment} tokens in
     * the respective modules.
     *
     * @since 0.20.0
     */
    @Parameter(property = "quarkus.nameSegmentDelimiter", defaultValue = DEFAULT_NAME_SEGMENT_DELIMITER)
    String nameSegmentDelimiter;

    /**
     * Base Java package under which Java classes should be created in Runtime and Deployment modules. If not set, the
     * Java package will be auto-generated out of {@link #groupId}, {@link #javaPackageInfix} and {@link #artifactId}
     *
     * @since 0.20.0
     */
    @Parameter(property = "quarkus.javaPackageBase")
    String javaPackageBase;

    /**
     * If {@link #javaPackageBase} is not set explicitly, this infix will be put between package segments taken from
     * {@link #groupId} and {@link #artifactId}.
     * <p>
     * Example: Given
     * <ul>
     * <li>{@link #groupId} is {@code org.example.quarkus.extensions}</li>
     * <li>{@link #javaPackageInfix} is {@code foo.bar}</li>
     * <li>{@link #artifactId} is {@code cool-extension}</li>
     * </ul>
     * Then the auto-generated {@link #javaPackageBase} will be
     * {@code org.example.quarkus.extensions.foo.bar.cool.extension}
     *
     * @since 0.20.0
     */
    @Parameter(property = "quarkus.javaPackageInfix")
    String javaPackageInfix;

    /**
     * This mojo creates a triple of Maven modules (Parent, Runtime and Deployment). "Grand parent" is the parent of the
     * Parent module. If {@code grandParentArtifactId} is left unset, the {@code artifactId} from the {@code pom.xml} in
     * the current directory will be used. Otherwise, an exception is thrown.
     *
     * @since 0.20.0
     */
    @Parameter(property = "quarkus.grandParentArtifactId")
    String grandParentArtifactId;

    /**
     * This mojo creates a triple of Maven modules (Parent, Runtime and Deployment). "Grand parent" is the parent of the
     * Parent module. If {@code grandParentGroupId} is left unset, the {@code groupId} from the {@code pom.xml} in the
     * current directory will be used. Otherwise, an exception is thrown.
     *
     * @since 0.20.0
     */
    @Parameter(property = "quarkus.grandParentGroupId")
    String grandParentGroupId;

    /**
     * This mojo creates a triple of Maven modules (Parent, Runtime and Deployment). "Grand parent" is the parent of the
     * Parent module. If {@code grandParentRelativePath} is left unset, the default {@code relativePath}
     * {@code "../pom.xml"} is used.
     *
     * @since 0.20.0
     */
    @Parameter(property = "quarkus.grandParentRelativePath")
    String grandParentRelativePath;

    /**
     * This mojo creates a triple of Maven modules (Parent, Runtime and Deployment). "Grand parent" is the parent of the
     * Parent module. If {@code grandParentVersion} is left unset, the {@code version} from the {@code pom.xml} in the
     * current directory will be used. Otherwise, an exception is thrown.
     *
     * @since 0.20.0
     */
    @Parameter(property = "quarkus.grandParentVersion")
    String grandParentVersion;

    /**
     * Quarkus version the newly created extension should depend on. If you want to pass a property placeholder, use
     * {@code @} instead if {@code $} so that the property is not evaluated by the current mojo - e.g.
     * <code>@{quarkus.version}</code>
     *
     * @since 0.20.0
     */
    @Parameter(defaultValue = DEFAULT_QUARKUS_VERSION, required = true, property = "quarkus.version")
    String quarkusVersion;

    /**
     * This parameter was introduced to change the property of {@code artifactId} parameter from {@code quarkus.artifactId} to
     * {@code artifactId}
     *
     * @since 1.3.0
     */
    @Deprecated
    @Parameter(property = "quarkus.quarkusVersion")
    String deprecatedQuarkusVersion;

    /**
     * If {@code true} the Maven dependencies in Runtime and Deployment modules will not have their versions set and the
     * {@code quarkus-bootstrap-maven-plugin} in the Runtime module will not have its version set and it will have no
     * executions configured. If {@code false} the version set in {@link #quarkusVersion} will be used where applicable
     * and {@code quarkus-bootstrap-maven-plugin} in the Runtime module will be configured explicitly. If the value is
     * {@code null} the parameter will be treated as it was initialized to {@code false}.
     *
     * @since 0.20.0
     */
    @Parameter(property = "quarkus.assumeManaged")
    Boolean assumeManaged;

    /**
     * URI prefix to use when looking up FreeMarker templates when generating various source files. You need to touch
     * this only if you want to provide your own custom templates.
     * <p>
     * The following URI schemes are supported:
     * <ul>
     * <li>{@code classpath:}</li>
     * <li>{@code file:} (relative to {@link #basedir})</li>
     * </ul>
     * These are the template files you may want to provide under your custom {@link #templatesUriBase}:
     * <ul>
     * <li>{@code deployment-pom.xml}</li>
     * <li>{@code parent-pom.xml}</li>
     * <li>{@code runtime-pom.xml}</li>
     * <li>{@code Processor.java}</li>
     * </ul>
     * Note that you do not need to provide all of them. Files not available in your custom {@link #templatesUriBase}
     * will be looked up in the default URI base {@value #DEFAULT_TEMPLATES_URI_BASE}. The default templates are
     * maintained <a href=
     * "https://github.com/quarkusio/quarkus/tree/master/devtools/maven/src/main/resources/create-extension-templates">here</a>.
     *
     * @since 0.20.0
     */
    @Parameter(defaultValue = DEFAULT_TEMPLATES_URI_BASE, required = true, property = "quarkus.templatesUriBase")
    String templatesUriBase;

    /**
     * Encoding to read and write files in the current source tree
     *
     * @since 0.20.0
     */
    @Parameter(defaultValue = DEFAULT_ENCODING, required = true, property = "quarkus.encoding")
    String encoding;

    /**
     * Path relative to {@link #basedir} pointing at a {@code pom.xml} file containing the BOM (Bill of Materials) that
     * manages runtime extension artifacts. If set, the newly created Runtime module will be added to
     * {@code <dependencyManagement>} section of this bom; otherwise the newly created Runtime module will not be added
     * to any BOM.
     *
     * @since 0.21.0
     */
    @Parameter(property = "quarkus.runtimeBomPath")
    Path runtimeBomPath;

    /**
     * Path relative to {@link #basedir} pointing at a {@code pom.xml} file containing the BOM (Bill of Materials) that
     * manages deployment time extension artifacts. If set, the newly created Deployment module will be added to
     * {@code <dependencyManagement>} section of this bom; otherwise the newly created Deployment module will not be
     * added to any BOM.
     *
     * @since 0.21.0
     */
    @Parameter(property = "quarkus.deploymentBomPath")
    Path deploymentBomPath;

    /**
     * A version for the entries added to the runtime BOM (see {@link #runtimeBomPath}) and to the deployment BOM (see
     * {@link #deploymentBomPath}). If you want to pass a property placeholder, use {@code @} instead if {@code $} so
     * that the property is not evaluated by the current mojo - e.g. <code>@{my-project.version}</code>
     *
     * @since 0.25.0
     */
    @Parameter(property = "quarkus.bomEntryVersion", defaultValue = DEFAULT_BOM_ENTRY_VERSION)
    String bomEntryVersion;

    /**
     * A list of strings of the form {@code groupId:artifactId:version[:type[:classifier[:scope]]]} representing the
     * dependencies that should be added to the generated runtime module and to the runtime BOM if it is specified via
     * {@link #runtimeBomPath}.
     * <p>
     * In case the built-in Maven <code>${placeholder}</code> expansion does not work well for you (because you e.g.
     * pass {@link #additionalRuntimeDependencies}) via CLI, the Mojo supports a custom <code>@{placeholder}</code>
     * expansion:
     * <ul>
     * <li><code>@{$}</code> will be expanded to {@code $} - handy for escaping standard placeholders. E.g. to insert
     * <code>${quarkus.version}</code> to the BOM, you need to pass <code>@{$}{quarkus.version}</code></li>
     * <li><code>@{quarkus.field}</code> will be expanded to whatever value the given {@code field} of this mojo has at
     * runtime.</li>
     * <li>Any other <code>@{placeholder}</code> will be resolved using the current project's properties</li>
     * </ul>
     *
     * @since 0.22.0
     */
    @Parameter(property = "quarkus.additionalRuntimeDependencies")
    List<String> additionalRuntimeDependencies;

    /**
     * A path relative to {@link #basedir} pointing at a {@code pom.xml} file that should serve as a parent for the
     * integration test Maven module this mojo generates. If {@link #itestParentPath} is not set, the integration test
     * module will not be generated.
     *
     * @since 0.22.0
     */
    @Parameter(property = "quarkus.itestParentPath")
    Path itestParentPath;

    @Parameter(defaultValue = "${project}", readonly = true)
    MavenProject project;

    /**
     * The entry point to Aether, i.e. the component doing all the work.
     *
     * @component
     */
    @Component
    private RepositorySystem repoSystem;

    /**
     * The current repository/network configuration of Maven.
     *
     * @parameter default-value="${repositorySystemSession}"
     * @readonly
     */
    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    /**
     * The project's remote repositories to use for the resolution of artifacts and their dependencies.
     *
     * @parameter default-value="${project.remoteProjectRepositories}"
     * @readonly
     */
    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    private List<RemoteRepository> repos;

    /**
     * The version of {@code org.apache.maven.plugins:maven-compiler-plugin} that should be used for
     * the extension project.
     */
    @Parameter(defaultValue = COMPILER_PLUGIN_DEFAULT_VERSION, required = true, property = "quarkus.mavenCompilerPluginVersion")
    String compilerPluginVersion;

    boolean currentProjectIsBaseDir;

    Charset charset;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        if (this.basedir == null) {
            currentProjectIsBaseDir = true;
            this.basedir = new File(".").getAbsoluteFile();
        }

        if (deprecatedGroupId != null) {
            if (groupId != null) {
                throw new MojoExecutionException("Either groupId or deprecatedGroupId can be set at a time but got groupId="
                        + groupId + " and deprecatedGroupId=" + deprecatedGroupId);
            }
            groupId = deprecatedGroupId;
        }
        if (deprecatedArtifactId != null) {
            if (artifactId != null) {
                throw new MojoExecutionException(
                        "Either artifactId or deprecatedArtifactId can be set at a time but got artifactId=" + artifactId
                                + " and deprecatedArtifactId=" + deprecatedArtifactId);
            }
            artifactId = deprecatedArtifactId;
        }
        if (deprecatedVersion != null) {
            if (version != null) {
                throw new MojoExecutionException("Either version or deprecatedVersion can be set at a time but got version="
                        + version + " and deprecatedVersion=" + deprecatedVersion);
            }
            version = deprecatedVersion;
        }
        if (deprecatedQuarkusVersion != null) {
            if (quarkusVersion != null && !DEFAULT_QUARKUS_VERSION.equals(quarkusVersion)) {
                throw new MojoExecutionException(
                        "Either quarkusVersion or deprecatedQuarkusVersion can be set at a time but got quarkusVersion="
                                + quarkusVersion + " and deprecatedQuarkusVersion=" + deprecatedQuarkusVersion);
            }
            quarkusVersion = deprecatedQuarkusVersion;
        }

        if (artifactId != null) {
            artifactIdBase = artifactIdBase(artifactId);
            artifactIdPrefix = artifactId.substring(0, artifactId.length() - artifactIdBase.length());
            artifactId = BRACKETS_PATTERN.matcher(artifactId).replaceAll("");
        } else if (artifactIdBase != null) {
            artifactId = artifactIdPrefix == null || artifactIdPrefix.isEmpty() ? artifactIdBase
                    : artifactIdPrefix + artifactIdBase;
        } else {
            throw new MojoFailureException(String.format(
                    "Either artifactId or both artifactIdPrefix and artifactIdBase must be specified; found: artifactId=[%s], artifactIdPrefix=[%s], artifactIdBase[%s]",
                    artifactId, artifactIdPrefix, artifactIdBase));
        }

        if (name != null) {
            final int pos = name.lastIndexOf(nameSegmentDelimiter);
            if (pos >= 0) {
                nameBase = name.substring(pos + nameSegmentDelimiter.length());
            } else {
                nameBase = name;
            }
            namePrefix = name.substring(0, name.length() - nameBase.length());
        } else {
            if (nameBase == null) {
                nameBase = toCapWords(artifactIdBase);
            }
            if (namePrefix == null) {
                namePrefix = "";
            }
            if (nameBase != null && namePrefix != null) {
                name = namePrefix + nameBase;
            } else {
                throw new MojoFailureException("Either name or both namePrefix and nameBase must be specified");
            }
        }

        if (runtimeBomPath != null) {
            runtimeBomPath = basedir.toPath().resolve(runtimeBomPath);
            if (!Files.exists(runtimeBomPath)) {
                throw new MojoFailureException("runtimeBomPath does not exist: " + runtimeBomPath);
            }
        }
        if (deploymentBomPath != null) {
            deploymentBomPath = basedir.toPath().resolve(deploymentBomPath);
            if (!Files.exists(deploymentBomPath)) {
                throw new MojoFailureException("deploymentBomPath does not exist: " + deploymentBomPath);
            }
        }

        charset = Charset.forName(encoding);

        try {
            File rootPom = null;
            Model rootModel = null;
            boolean importDeploymentBom = true;
            boolean setCompilerPluginVersion = true;
            boolean setQuarkusVersionProp = true;
            if (isCurrentProjectExists()) {
                rootPom = getCurrentProjectPom();
                rootModel = MojoUtils.readPom(rootPom);
                if (!"pom".equals(rootModel.getPackaging())) {
                    throw new MojoFailureException(
                            "Can add extension modules only under a project with packaging 'pom'; found: "
                                    + rootModel.getPackaging() + "");
                }

                if (rootPom.equals(project.getFile())) {
                    importDeploymentBom = !hasQuarkusDeploymentBom();
                    setCompilerPluginVersion = !project.getPluginManagement().getPluginsAsMap()
                            .containsKey(COMPILER_PLUGIN_KEY);
                    setQuarkusVersionProp = !project.getProperties().containsKey(QUARKUS_VERSION_PROP);
                } else {
                    // aloubyansky: not sure we should support this case and not sure it ever worked properly
                    // this is about creating an extension project not in the context of the current project from the Maven's plugin perspective
                    // kind of a pathological use-case from the Maven's perspective, imo
                    final DefaultArtifact rootArtifact = new DefaultArtifact(getGroupId(rootModel),
                            rootModel.getArtifactId(), null, rootModel.getPackaging(), getVersion(rootModel));
                    try {
                        final LocalWorkspace ws = LocalProject.loadWorkspace(rootPom.getParentFile().toPath()).getWorkspace();
                        final MavenArtifactResolver mvn = MavenArtifactResolver.builder()
                                .setRepositorySystem(MavenRepoInitializer.getRepositorySystem(repoSession.isOffline(), ws))
                                .setRepositorySystemSession(repoSession)
                                .setRemoteRepositories(repos)
                                .setWorkspace(LocalProject.loadWorkspace(rootPom.getParentFile().toPath()).getWorkspace())
                                .build();
                        final ArtifactDescriptorResult rootDescr = mvn.resolveDescriptor(rootArtifact);
                        importDeploymentBom = !hasQuarkusDeploymentBom(rootDescr.getManagedDependencies());
                        // TODO determine whether the compiler plugin is configured for the project
                        setQuarkusVersionProp = !rootDescr.getProperties().containsKey(QUARKUS_VERSION_PROP);
                    } catch (Exception e) {
                        throw new MojoExecutionException("Failed to resolve " + rootArtifact + " descriptor", e);
                    }
                }
            } else if (this.grandParentRelativePath != null) {
                // aloubyansky: not sure we should support this case, same as above
                final File gpPom = getExtensionProjectBaseDir().resolve(this.grandParentRelativePath).normalize()
                        .toAbsolutePath().toFile();
                if (gpPom.exists()) {
                    rootPom = gpPom;
                    rootModel = MojoUtils.readPom(gpPom);
                    final DefaultArtifact rootArtifact = new DefaultArtifact(getGroupId(rootModel),
                            rootModel.getArtifactId(), null, rootModel.getPackaging(), getVersion(rootModel));
                    try {
                        final LocalWorkspace ws = LocalProject.loadWorkspace(rootPom.getParentFile().toPath()).getWorkspace();
                        final MavenArtifactResolver mvn = MavenArtifactResolver.builder()
                                .setRepositorySystem(MavenRepoInitializer.getRepositorySystem(repoSession.isOffline(), ws))
                                .setRepositorySystemSession(repoSession)
                                .setRemoteRepositories(repos)
                                .setWorkspace(ws)
                                .build();
                        final ArtifactDescriptorResult rootDescr = mvn.resolveDescriptor(rootArtifact);
                        importDeploymentBom = !hasQuarkusDeploymentBom(rootDescr.getManagedDependencies());
                        // TODO determine whether the compiler plugin is configured for the project
                        setQuarkusVersionProp = !rootDescr.getProperties().containsKey(QUARKUS_VERSION_PROP);
                    } catch (Exception e) {
                        throw new MojoExecutionException("Failed to resolve " + rootArtifact + " descriptor", e);
                    }
                }
            }

            final TemplateParams templateParams = getTemplateParams(rootModel);
            final Configuration cfg = getTemplateConfig();

            generateExtensionProjects(cfg, templateParams);
            if (setQuarkusVersionProp) {
                setQuarkusVersionProp(getExtensionProjectBaseDir().resolve("pom.xml").toFile());
            }
            if (importDeploymentBom) {
                addQuarkusDeploymentBom(getExtensionProjectBaseDir().resolve("pom.xml").toFile());
            }
            if (setCompilerPluginVersion) {
                setCompilerPluginVersion(getExtensionProjectBaseDir().resolve("pom.xml").toFile());
            }
            if (rootModel != null) {
                addModules(rootPom.toPath(), templateParams, rootModel);
            }

            if (runtimeBomPath != null) {
                getLog().info(
                        String.format("Adding [%s] to dependencyManagement in [%s]", templateParams.artifactId,
                                runtimeBomPath));
                List<PomTransformer.Transformation> transformations = new ArrayList<PomTransformer.Transformation>();
                transformations
                        .add(Transformation.addManagedDependency(templateParams.groupId, templateParams.artifactId,
                                templateParams.bomEntryVersion));
                for (Gavtcs gavtcs : templateParams.additionalRuntimeDependencies) {
                    getLog().info(String.format("Adding [%s] to dependencyManagement in [%s]", gavtcs, runtimeBomPath));
                    transformations.add(Transformation.addManagedDependency(gavtcs));
                }
                pomTransformer(runtimeBomPath).transform(transformations);
            }
            if (deploymentBomPath != null) {
                final String aId = templateParams.artifactId + "-deployment";
                getLog().info(String.format("Adding [%s] to dependencyManagement in [%s]", aId, deploymentBomPath));
                pomTransformer(deploymentBomPath)
                        .transform(Transformation.addManagedDependency(templateParams.groupId, aId,
                                templateParams.bomEntryVersion));
            }
            if (itestParentPath != null) {
                generateItest(cfg, templateParams);
            }
        } catch (IOException e) {
            throw new MojoExecutionException(String.format("Could not read %s", project.getFile()), e);
        } catch (TemplateException e) {
            throw new MojoExecutionException(String.format("Could not process a FreeMarker template"), e);
        }
    }

    private void setQuarkusVersionProp(File pom) throws IOException, MojoExecutionException {
        pomTransformer(pom.toPath()).transform(Transformation.addProperty(QUARKUS_VERSION_PROP,
                quarkusVersion.equals(DEFAULT_QUARKUS_VERSION) ? getPluginVersion() : quarkusVersion));
    }

    private void setCompilerPluginVersion(File pom) throws IOException {
        pomTransformer(pom.toPath()).transform(Transformation.addProperty(COMPILER_PLUGIN_VERSION_PROP, compilerPluginVersion));
        pomTransformer(pom.toPath())
                .transform(Transformation.addManagedPlugin(
                        MojoUtils.plugin(COMPILER_PLUGIN_GROUP_ID, COMPILER_PLUGIN_ARTIFACT_ID,
                                COMPILER_PLUGIN_VERSION_POM_EXPR)));
    }

    private void addQuarkusDeploymentBom(File pom) throws IOException, MojoExecutionException {
        addQuarkusDeploymentBom(MojoUtils.readPom(pom), pom);
    }

    private void addQuarkusDeploymentBom(Model model, File file) throws IOException, MojoExecutionException {
        pomTransformer(file.toPath())
                .transform(Transformation.addManagedDependency(
                        new Gavtcs(platformGroupId, platformArtifactId, QUARKUS_VERSION_POM_EXPR, "pom", null, "import")));
    }

    private String getPluginVersion() throws MojoExecutionException {
        return CreateUtils.resolvePluginInfo(CreateExtensionMojo.class).getVersion();
    }

    private boolean hasQuarkusDeploymentBom() {
        if (project.getDependencyManagement() == null) {
            return false;
        }
        for (org.apache.maven.model.Dependency dep : project.getDependencyManagement().getDependencies()) {
            if (dep.getArtifactId().equals("quarkus-core-deployment")
                    && dep.getGroupId().equals("io.quarkus")) {
                // this is not a 100% accurate check but practically valid
                return true;
            }
        }
        return false;
    }

    private boolean hasQuarkusDeploymentBom(List<Dependency> deps) {
        if (deps == null) {
            return false;
        }
        for (Dependency dep : deps) {
            if (dep.getArtifact().getArtifactId().equals("quarkus-core-deployment")
                    && dep.getArtifact().getGroupId().equals("io.quarkus")) {
                // this is not a 100% accurate check but practically valid
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if the goal is executed in an existing project
     */
    private boolean isCurrentProjectExists() {
        return currentProjectIsBaseDir ? project.getFile() != null
                : Files.exists(basedir.toPath().resolve("pom.xml"));
    }

    private File getCurrentProjectPom() {
        if (currentProjectIsBaseDir) {
            return project.getFile() == null ? new File(project.getBasedir(), "pom.xml") : project.getFile();
        }
        return new File(basedir, "pom.xml");
    }

    private Path getExtensionProjectBaseDir() {
        if (currentProjectIsBaseDir) {
            return project.getBasedir() == null ? basedir.toPath().resolve(artifactIdBase)
                    : project.getBasedir().toPath().resolve(artifactIdBase);
        }
        return new File(basedir, artifactIdBase).toPath();
    }

    private Path getExtensionRuntimeBaseDir() {
        return getExtensionProjectBaseDir().resolve("runtime");
    }

    private Path getExtensionDeploymentBaseDir() {
        return getExtensionProjectBaseDir().resolve("deployment");
    }

    void addModules(Path basePomXml, TemplateParams templateParams, Model basePom)
            throws IOException, TemplateException, MojoFailureException, MojoExecutionException {
        if (!basePom.getModules().contains(templateParams.artifactIdBase)) {
            getLog().info(String.format("Adding module [%s] to [%s]", templateParams.artifactIdBase, basePomXml));
            pomTransformer(basePomXml).transform(Transformation.addModule(templateParams.artifactIdBase));
        }
    }

    private void generateExtensionProjects(Configuration cfg, TemplateParams templateParams)
            throws IOException, TemplateException, MojoExecutionException {
        evalTemplate(cfg, "parent-pom.xml", getExtensionProjectBaseDir().resolve("pom.xml"), templateParams);

        Files.createDirectories(
                getExtensionRuntimeBaseDir().resolve("src/main/java")
                        .resolve(templateParams.javaPackageBase.replace('.', '/')));
        evalTemplate(cfg, "runtime-pom.xml", getExtensionRuntimeBaseDir().resolve("pom.xml"),
                templateParams);

        evalTemplate(cfg, "deployment-pom.xml", getExtensionDeploymentBaseDir().resolve("pom.xml"),
                templateParams);
        final Path processorPath = getExtensionDeploymentBaseDir()
                .resolve("src/main/java")
                .resolve(templateParams.javaPackageBase.replace('.', '/'))
                .resolve("deployment")
                .resolve(templateParams.artifactIdBaseCamelCase + "Processor.java");
        evalTemplate(cfg, "Processor.java", processorPath, templateParams);
    }

    private PomTransformer pomTransformer(Path basePomXml) {
        return new PomTransformer(basePomXml, charset);
    }

    private TemplateParams getTemplateParams(Model basePom) throws MojoExecutionException {
        final TemplateParams templateParams = new TemplateParams();

        templateParams.artifactId = artifactId;
        templateParams.artifactIdPrefix = artifactIdPrefix;
        templateParams.artifactIdBase = artifactIdBase;
        templateParams.artifactIdBaseCamelCase = toCapCamelCase(templateParams.artifactIdBase);

        if (groupId == null) {
            if (basePom == null) {
                throw new MojoExecutionException(
                        "Please provide the desired groupId for the project by setting groupId parameter");
            }
            templateParams.groupId = getGroupId(basePom);
        } else {
            templateParams.groupId = groupId;
        }

        if (version == null) {
            if (basePom == null) {
                throw new MojoExecutionException(
                        "Please provide the desired version for the project by setting version parameter");
            }
            templateParams.version = getVersion(basePom);
        } else {
            templateParams.version = version;
        }

        templateParams.namePrefix = namePrefix;
        templateParams.nameBase = nameBase;
        templateParams.nameSegmentDelimiter = nameSegmentDelimiter;
        templateParams.assumeManaged = detectAssumeManaged();
        templateParams.quarkusVersion = QUARKUS_VERSION_POM_EXPR;
        templateParams.bomEntryVersion = bomEntryVersion.replace('@', '$');

        if (basePom != null) {
            templateParams.grandParentGroupId = grandParentGroupId != null ? grandParentGroupId : getGroupId(basePom);
            templateParams.grandParentArtifactId = grandParentArtifactId != null ? grandParentArtifactId
                    : basePom.getArtifactId();
            templateParams.grandParentVersion = grandParentVersion != null ? grandParentVersion : getVersion(basePom);
            templateParams.grandParentRelativePath = grandParentRelativePath != null ? grandParentRelativePath : "../pom.xml";
        }

        templateParams.javaPackageBase = javaPackageBase != null ? javaPackageBase
                : getJavaPackage(templateParams.groupId, javaPackageInfix, artifactId);
        templateParams.additionalRuntimeDependencies = getAdditionalRuntimeDependencies();
        templateParams.runtimeBomPathSet = runtimeBomPath != null;
        return templateParams;
    }

    private Configuration getTemplateConfig() throws IOException {
        final Configuration templateCfg = new Configuration(Configuration.VERSION_2_3_28);
        templateCfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        templateCfg.setTemplateLoader(createTemplateLoader(basedir, templatesUriBase));
        templateCfg.setDefaultEncoding(encoding);
        templateCfg.setInterpolationSyntax(Configuration.SQUARE_BRACKET_INTERPOLATION_SYNTAX);
        templateCfg.setTagSyntax(Configuration.SQUARE_BRACKET_TAG_SYNTAX);
        return templateCfg;
    }

    void generateItest(Configuration cfg, TemplateParams model)
            throws MojoFailureException, MojoExecutionException, TemplateException, IOException {
        final Path itestParentAbsPath = basedir.toPath().resolve(itestParentPath);
        try (Reader r = Files.newBufferedReader(itestParentAbsPath, charset)) {
            final Model itestParent = new MavenXpp3Reader().read(r);
            if (!"pom".equals(itestParent.getPackaging())) {
                throw new MojoFailureException(
                        "Can add an extension integration test only under a project with packagin 'pom'; found: "
                                + itestParent.getPackaging() + " in " + itestParentAbsPath);
            }
            model.itestParentGroupId = getGroupId(itestParent);
            model.itestParentArtifactId = itestParent.getArtifactId();
            model.itestParentVersion = getVersion(itestParent);
            model.itestParentRelativePath = "../pom.xml";

            final Path itestDir = itestParentAbsPath.getParent().resolve(model.artifactIdBase);
            evalTemplate(cfg, "integration-test-pom.xml", itestDir.resolve("pom.xml"), model);

            final Path testResourcePath = itestDir.resolve("src/main/java/" + model.javaPackageBase.replace('.', '/')
                    + "/it/" + model.artifactIdBaseCamelCase + "Resource.java");
            evalTemplate(cfg, "TestResource.java", testResourcePath, model);
            final Path testClassDir = itestDir
                    .resolve("src/test/java/" + model.javaPackageBase.replace('.', '/') + "/it");
            evalTemplate(cfg, "Test.java", testClassDir.resolve(model.artifactIdBaseCamelCase + "Test.java"),
                    model);
            evalTemplate(cfg, "IT.java", testClassDir.resolve(model.artifactIdBaseCamelCase + "IT.java"),
                    model);

            getLog().info(String.format("Adding module [%s] to [%s]", model.artifactIdBase, itestParentAbsPath));
            pomTransformer(itestParentAbsPath).transform(Transformation.addModule(model.artifactIdBase));

        } catch (IOException e) {
            throw new MojoExecutionException(String.format("Could not read %s", itestParentAbsPath), e);
        } catch (XmlPullParserException e) {
            throw new MojoExecutionException(String.format("Could not parse %s", itestParentAbsPath), e);
        }
    }

    private List<Gavtcs> getAdditionalRuntimeDependencies() {
        final List<Gavtcs> result = new ArrayList<>();
        if (additionalRuntimeDependencies != null && !additionalRuntimeDependencies.isEmpty()) {
            for (String rawGavtc : additionalRuntimeDependencies) {
                rawGavtc = replacePlaceholders(rawGavtc);
                result.add(Gavtcs.of(rawGavtc));
            }
        }
        return result;
    }

    private String replacePlaceholders(String gavtc) {
        final StringBuffer transformedGavtc = new StringBuffer();
        final Matcher m = PLACEHOLDER_PATTERN.matcher(gavtc);
        while (m.find()) {
            final String key = m.group(1);
            if ("$".equals(key)) {
                m.appendReplacement(transformedGavtc, QUOTED_DOLLAR);
            } else if (key.startsWith("quarkus.")) {
                final String fieldName = key.substring("quarkus.".length());
                try {
                    final Field field = this.getClass().getDeclaredField(fieldName);
                    Object val = field.get(this);
                    if (val != null) {
                        m.appendReplacement(transformedGavtc, String.valueOf(val));
                    }
                } catch (NoSuchFieldException | SecurityException | IllegalArgumentException
                        | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            } else {
                final Object val = project.getProperties().get(key);
                if (val != null) {
                    m.appendReplacement(transformedGavtc, String.valueOf(val));
                }
            }
        }
        m.appendTail(transformedGavtc);
        return transformedGavtc.toString();
    }

    boolean detectAssumeManaged() {
        return assumeManaged == null ? false : assumeManaged;
    }

    static String getGroupId(Model basePom) {
        return basePom.getGroupId() != null ? basePom.getGroupId()
                : basePom.getParent() != null && basePom.getParent().getGroupId() != null
                        ? basePom.getParent().getGroupId()
                        : null;
    }

    static String getVersion(Model basePom) {
        return basePom.getVersion() != null ? basePom.getVersion()
                : basePom.getParent() != null && basePom.getParent().getVersion() != null
                        ? basePom.getParent().getVersion()
                        : null;
    }

    static String toCapCamelCase(String artifactIdBase) {
        final StringBuilder sb = new StringBuilder(artifactIdBase.length());
        for (String segment : artifactIdBase.split("[.\\-]+")) {
            sb.append(Character.toUpperCase(segment.charAt(0)));
            if (segment.length() > 1) {
                sb.append(segment.substring(1));
            }
        }
        return sb.toString();
    }

    static String toCapWords(String artifactIdBase) {
        final StringBuilder sb = new StringBuilder(artifactIdBase.length());
        for (String segment : artifactIdBase.split("[.\\-]+")) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(segment.charAt(0)));
            if (segment.length() > 1) {
                sb.append(segment.substring(1));
            }
        }
        return sb.toString();
    }

    static String getJavaPackage(String groupId, String javaPackageInfix, String artifactId) {
        final Stack<String> segments = new Stack<>();
        for (String segment : groupId.split("[.\\-]+")) {
            if (segments.isEmpty() || !segments.peek().equals(segment)) {
                segments.add(segment);
            }
        }
        if (javaPackageInfix != null) {
            for (String segment : javaPackageInfix.split("[.\\-]+")) {
                segments.add(segment);
            }
        }
        for (String segment : artifactId.split("[.\\-]+")) {
            if (!segments.contains(segment)) {
                segments.add(segment);
            }
        }
        return segments.stream() //
                .map(s -> s.toLowerCase(Locale.ROOT)) //
                .map(s -> SourceVersion.isKeyword(s) ? s + "_" : s) //
                .collect(Collectors.joining("."));
    }

    static TemplateLoader createTemplateLoader(File basedir, String templatesUriBase) throws IOException {
        final TemplateLoader defaultLoader = new ClassTemplateLoader(CreateExtensionMojo.class,
                DEFAULT_TEMPLATES_URI_BASE.substring(CLASSPATH_PREFIX.length()));
        if (DEFAULT_TEMPLATES_URI_BASE.equals(templatesUriBase)) {
            return defaultLoader;
        } else if (templatesUriBase.startsWith(CLASSPATH_PREFIX)) {
            return new MultiTemplateLoader( //
                    new TemplateLoader[] { //
                            new ClassTemplateLoader(CreateExtensionMojo.class,
                                    templatesUriBase.substring(CLASSPATH_PREFIX.length())), //
                            defaultLoader //
                    });
        } else if (templatesUriBase.startsWith(FILE_PREFIX)) {
            return new MultiTemplateLoader( //
                    new TemplateLoader[] { //
                            new FileTemplateLoader(
                                    new File(basedir, templatesUriBase.substring(FILE_PREFIX.length()))), //
                            defaultLoader //
                    });
        } else {
            throw new IllegalStateException(String.format(
                    "Cannot handle templatesUriBase '%s'; only value starting with '%s' or '%s' are supported",
                    templatesUriBase, CLASSPATH_PREFIX, FILE_PREFIX));
        }
    }

    static void evalTemplate(Configuration cfg, String templateUri, Path dest, TemplateParams model)
            throws IOException, TemplateException {
        log.infof("Adding '%s'", dest);
        final Template template = cfg.getTemplate(templateUri);
        Files.createDirectories(dest.getParent());
        try (Writer out = Files.newBufferedWriter(dest)) {
            template.process(model, out);
        }
    }

    static String artifactIdBase(String artifactId) {
        final int lBPos = artifactId.indexOf('(');
        final int rBPos = artifactId.indexOf(')');
        if (lBPos >= 0 && rBPos >= 0) {
            return artifactId.substring(lBPos + 1, rBPos);
        } else {
            return artifactId;
        }
    }

    public void setRuntimeBomPath(String runtimeBomPath) {
        this.runtimeBomPath = Paths.get(runtimeBomPath);
    }

    public void setDeploymentBomPath(String deploymentBomPath) {
        this.deploymentBomPath = Paths.get(deploymentBomPath);
    }

    public void setItestParentPath(String itestParentPath) {
        this.itestParentPath = Paths.get(itestParentPath);
    }

    private void debug(String format, Object... args) {
        if (getLog().isDebugEnabled()) {
            getLog().debug(String.format(format, args));
        }
    }

    public static class TemplateParams {
        String grandParentRelativePath;
        String grandParentVersion;
        String grandParentArtifactId;
        String grandParentGroupId;
        String itestParentRelativePath;
        String itestParentVersion;
        String itestParentArtifactId;
        String itestParentGroupId;
        String groupId;
        String artifactId;
        String artifactIdPrefix;
        String artifactIdBase;
        String artifactIdBaseCamelCase;
        String version;
        String namePrefix;
        String nameBase;
        String nameSegmentDelimiter;
        String javaPackageBase;
        boolean assumeManaged;
        String quarkusVersion;
        List<Gavtcs> additionalRuntimeDependencies;
        boolean runtimeBomPathSet;
        String bomEntryVersion;

        public String getJavaPackageBase() {
            return javaPackageBase;
        }

        public boolean isAssumeManaged() {
            return assumeManaged;
        }

        public String getArtifactIdPrefix() {
            return artifactIdPrefix;
        }

        public String getArtifactIdBase() {
            return artifactIdBase;
        }

        public String getNamePrefix() {
            return namePrefix;
        }

        public String getNameBase() {
            return nameBase;
        }

        public String getNameSegmentDelimiter() {
            return nameSegmentDelimiter;
        }

        public String getArtifactIdBaseCamelCase() {
            return artifactIdBaseCamelCase;
        }

        public String getQuarkusVersion() {
            return quarkusVersion;
        }

        public String getGrandParentRelativePath() {
            return grandParentRelativePath;
        }

        public String getGrandParentVersion() {
            return grandParentVersion;
        }

        public String getGrandParentArtifactId() {
            return grandParentArtifactId;
        }

        public String getGrandParentGroupId() {
            return grandParentGroupId;
        }

        public String getGroupId() {
            return groupId;
        }

        public String getVersion() {
            return version;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public List<Gavtcs> getAdditionalRuntimeDependencies() {
            return additionalRuntimeDependencies;
        }

        public boolean isRuntimeBomPathSet() {
            return runtimeBomPathSet;
        }

        public String getItestParentRelativePath() {
            return itestParentRelativePath;
        }

        public String getItestParentVersion() {
            return itestParentVersion;
        }

        public String getItestParentArtifactId() {
            return itestParentArtifactId;
        }

        public String getItestParentGroupId() {
            return itestParentGroupId;
        }
    }
}
