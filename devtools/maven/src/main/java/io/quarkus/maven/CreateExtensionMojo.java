package io.quarkus.maven;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Stack;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.lang.model.SourceVersion;

import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import io.quarkus.maven.utilities.PomTransformer;
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

    private static final Logger log = LoggerFactory.getLogger(CreateExtensionMojo.class);

    private static final Pattern BRACKETS_PATTERN = Pattern.compile("[()]+");
    private static final String CLASSPATH_PREFIX = "classpath:";
    private static final String FILE_PREFIX = "file:";

    static final String DEFAULT_ENCODING = "utf-8";
    static final String DEFAULT_QUARKUS_VERSION = "@{quarkus.version}";
    static final String DEFAULT_TEMPLATES_URI_BASE = "classpath:/create-extension-templates";
    static final String DEFAULT_NAME_SEGMENT_DELIMITER = " - ";

    /**
     * Directory where the changes should be performed. Default is the current directory of the current Java process.
     * 
     * @since 0.20.0
     */
    @Parameter(property = "quarkus.basedir")
    Path basedir;

    /**
     * The {@code groupId} for the newly created Maven modules. If {@code groupId} is left unset, the {@code groupId}
     * from the {@code pom.xml} in the current directory will be used. Otherwise, an exception is thrown.
     *
     * @since 0.20.0
     */
    @Parameter(property = "quarkus.groupId")
    String groupId;

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
    @Parameter(property = "quarkus.artifactId")
    String artifactId;

    /**
     * A prefix common to all extension artifactIds in the current source tree. If you set {@link #artifactIdPrefix},
     * set also {@link #artifactIdBase}, but do not set {@link #artifactId}.
     *
     * @since 0.20.0
     */
    @Parameter(property = "quarkus.artifactIdPrefix")
    String artifactIdPrefix;

    /**
     * The unique part of the {@link #artifactId}. If you set {@link #artifactIdBase}, set also
     * {@link #artifactIdPrefix}, but do not set {@link #artifactId}.
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
    @Parameter(property = "quarkus.artifactVersion")
    String version;

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
    @Parameter(defaultValue = DEFAULT_QUARKUS_VERSION, required = true, property = "quarkus.quarkusVersion")
    String quarkusVersion;

    /**
     * If {@code true} the Maven dependencies in Runtime and Deployment modules will not have their versions set and the
     * {@code quarkus-bootstrap-maven-plugin} in the Runtime module will not have its version set and it will have no
     * executions configured. If {@code false} the version set in {@link #quarkusVersion} will be used where applicable
     * and {@code quarkus-bootstrap-maven-plugin} in the Runtime module will be configured explicitly. If the value is
     * {@code null} the mojo attempts to autodetect the value inspecting the POM hierarchy of the current project: The
     * value is {@code true} if {@code quarkus-bootstrap-maven-plugin} is defined in the {@code pluginManagement}
     * section of the effective model of the current Maven module; otherwise the value is {@code false}.
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

    @Parameter(defaultValue = "${project}", readonly = true)
    MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        if (this.basedir == null) {
            this.basedir = Paths.get(".").toAbsolutePath().normalize();
        }
        if (artifactId != null) {
            artifactIdBase = artifactIdBase(artifactId);
            artifactIdPrefix = artifactId.substring(0, artifactId.length() - artifactIdBase.length());
            artifactId = BRACKETS_PATTERN.matcher(artifactId).replaceAll("");
        } else if (artifactIdPrefix != null && artifactIdBase != null) {
            artifactId = artifactIdPrefix + artifactIdBase;
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

        final Charset charset = Charset.forName(encoding);

        final Path basePomXml = basedir.resolve("pom.xml");
        if (Files.exists(basePomXml)) {
            try (Reader r = Files.newBufferedReader(basePomXml, charset)) {
                Model basePom = new MavenXpp3Reader().read(r);
                if (!"pom".equals(basePom.getPackaging())) {
                    throw new MojoFailureException(
                            "Can add extensiopn modules only under a project with packagin 'pom'; found: "
                                    + basePom.getPackaging() + "");
                }
                addModules(basePomXml, basePom, charset);
            } catch (IOException e) {
                throw new MojoExecutionException(String.format("Could not read %s", basePomXml), e);
            } catch (XmlPullParserException e) {
                throw new MojoExecutionException(String.format("Could not parse %s", basePomXml), e);
            } catch (TemplateException e) {
                throw new MojoExecutionException(String.format("Could not process a FreeMarker template"), e);
            }
        } else {
            newParent(basedir);
        }
    }

    void addModules(Path basePomXml, Model basePom, Charset charset) throws IOException, TemplateException {

        final Configuration cfg = new Configuration(Configuration.VERSION_2_3_28);
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setTemplateLoader(createTemplateLoader(basedir, templatesUriBase));
        cfg.setDefaultEncoding(charset.name());
        cfg.setInterpolationSyntax(Configuration.SQUARE_BRACKET_INTERPOLATION_SYNTAX);
        cfg.setTagSyntax(Configuration.SQUARE_BRACKET_TAG_SYNTAX);

        TemplateParams model = new TemplateParams();

        model.artifactId = artifactId;
        model.artifactIdPrefix = artifactIdPrefix;
        model.artifactIdBase = artifactIdBase;
        model.artifactIdBaseCamelCase = toCapCamelCase(model.artifactIdBase);

        model.groupId = this.groupId != null ? this.groupId : getGroupId(basePom);
        model.version = this.version != null ? this.version : getVersion(basePom);

        model.namePrefix = namePrefix;
        model.nameBase = nameBase;
        model.nameSegmentDelimiter = nameSegmentDelimiter;
        model.assumeManaged = detectAssumeManaged();
        model.quarkusVersion = quarkusVersion.replace('@', '$');

        model.grandParentGroupId = grandParentGroupId != null ? grandParentGroupId : getGroupId(basePom);
        model.grandParentArtifactId = grandParentArtifactId != null ? grandParentArtifactId : basePom.getArtifactId();
        model.grandParentVersion = grandParentVersion != null ? grandParentVersion : getVersion(basePom);
        model.grandParentRelativePath = grandParentRelativePath != null ? grandParentRelativePath : "../pom.xml";
        model.javaPackageBase = javaPackageBase != null ? javaPackageBase
                : getJavaPackage(model.groupId, javaPackageInfix, artifactId);

        evalTemplate(cfg, "parent-pom.xml", basedir.resolve(model.artifactIdBase + "/pom.xml"), charset, model);

        Files.createDirectories(basedir
                .resolve(model.artifactIdBase + "/runtime/src/main/java/" + model.javaPackageBase.replace('.', '/')));
        evalTemplate(cfg, "runtime-pom.xml", basedir.resolve(model.artifactIdBase + "/runtime/pom.xml"), charset,
                model);

        evalTemplate(cfg, "deployment-pom.xml", basedir.resolve(model.artifactIdBase + "/deployment/pom.xml"), charset,
                model);
        final Path processorPath = basedir
                .resolve(model.artifactIdBase + "/deployment/src/main/java/" + model.javaPackageBase.replace('.', '/')
                        + "/deployment/" + model.artifactIdBaseCamelCase + "Processor.java");
        evalTemplate(cfg, "Processor.java", processorPath, charset, model);

        if (!basePom.getModules().contains(model.artifactIdBase)) {
            new PomTransformer(basePomXml, charset).transform(Transformation.addModule(model.artifactIdBase));
        }

    }

    boolean detectAssumeManaged() {
        if (assumeManaged != null) {
            return assumeManaged.booleanValue();
        } else {
            if (project != null && project.getPluginManagement() != null
                    && project.getPluginManagement().getPlugins() != null) {
                for (Plugin plugin : project.getPluginManagement().getPlugins()) {
                    if ("io.quarkus".equals(plugin.getGroupId())
                            && "quarkus-bootstrap-maven-plugin".equals(plugin.getArtifactId())) {
                        return true;
                    }
                }
            }
            return false;
        }
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

    void newParent(Path path) {
        throw new UnsupportedOperationException(
                "Creating standalone extension projects is not supported yet. Only adding modules under and existing pom.xml file is supported.");
    }

    static TemplateLoader createTemplateLoader(Path basedir, String templatesUriBase) throws IOException {
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
                                    basedir.resolve(templatesUriBase.substring(FILE_PREFIX.length())).toFile()), //
                            defaultLoader //
                    });
        } else {
            throw new IllegalStateException(String.format(
                    "Cannot handle templatesUriBase '%s'; only value starting with '%s' or '%s' are supported",
                    templatesUriBase, CLASSPATH_PREFIX, FILE_PREFIX));
        }
    }

    static void evalTemplate(Configuration cfg, String templateUri, Path dest, Charset charset, TemplateParams model)
            throws IOException, TemplateException {
        log.info("Adding '{}'", dest);
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

    public static class TemplateParams {
        String grandParentRelativePath;
        String grandParentVersion;
        String grandParentArtifactId;
        String grandParentGroupId;
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
    }
}
