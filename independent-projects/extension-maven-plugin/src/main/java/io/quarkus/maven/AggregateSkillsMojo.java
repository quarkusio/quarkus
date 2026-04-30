package io.quarkus.maven;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexReader;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.Type;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.quarkus.devtools.utils.SkillComposer;

/**
 * Aggregates all extension skill files from the Quarkus source tree into a single JAR.
 * <p>
 * This goal walks the {@code extensionsDir} looking for
 * {@code {ext}/deployment/src/main/resources/META-INF/quarkus-skill.md} files.
 * For each one found, it reads the corresponding runtime extension metadata from
 * {@code {ext}/runtime/src/main/resources/META-INF/quarkus-extension.yaml},
 * composes them into a {@code SKILL.md} with YAML frontmatter per the
 * <a href="https://agentskills.io/specification">Agent Skills specification</a>,
 * and writes the result to {@code target/classes/META-INF/skills/{name}/SKILL.md}.
 * <p>
 * Extension developers only need to create a {@code quarkus-skill.md} file in their
 * deployment module's resources — no pom.xml configuration is required. This mojo
 * discovers all skill files automatically by scanning the source tree.
 */
@Mojo(name = "aggregate-skills", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public class AggregateSkillsMojo extends AbstractMojo {

    private static final String DEPLOYMENT = "deployment";
    private static final String RUNTIME = "runtime";

    @Parameter(readonly = true, required = true, defaultValue = "${project.build.outputDirectory}")
    private File outputDirectory;

    /**
     * Root directory containing Quarkus extensions.
     * Defaults to {@code ../../extensions} relative to the project base directory
     * (i.e. from {@code devtools/extension-skills/}).
     */
    @Parameter(defaultValue = "${project.basedir}/../../extensions", property = "extensionsDir")
    private File extensionsDir;

    @Override
    public void execute() throws MojoExecutionException {
        final Path extRoot = extensionsDir.toPath().normalize();
        if (!Files.isDirectory(extRoot)) {
            getLog().warn("Extensions directory does not exist: " + extRoot);
            return;
        }

        final List<SkillEntry> entries = discoverSkillFiles(extRoot);
        if (entries.isEmpty()) {
            getLog().info("No extension skill files found");
            return;
        }

        int composed = 0;
        for (SkillEntry entry : entries) {
            try {
                composeAndWrite(entry);
                composed++;
            } catch (IOException e) {
                getLog().warn("Failed to compose skill for " + entry.skillFile + ": " + e.getMessage());
            }
        }
        getLog().info("Aggregated " + composed + " extension skills from " + extRoot);
    }

    private List<SkillEntry> discoverSkillFiles(Path extRoot) throws MojoExecutionException {
        final List<SkillEntry> entries = new ArrayList<>();

        try {
            Files.walkFileTree(extRoot, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    // Match: **/deployment/src/main/resources/META-INF/quarkus-skill.md
                    if (file.getFileName().toString().equals("quarkus-skill.md")
                            && file.getParent() != null
                            && file.getParent().getFileName().toString().equals("META-INF")
                            && isDeploymentResourcePath(file)) {

                        Path runtimeYaml = resolveRuntimeMetadata(file);
                        if (runtimeYaml != null && Files.isRegularFile(runtimeYaml)) {
                            entries.add(new SkillEntry(file, runtimeYaml));
                        } else {
                            getLog().debug("No runtime quarkus-extension.yaml found for: " + file);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    // Skip target directories for performance
                    if (dir.getFileName().toString().equals("target")) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to scan extensions directory: " + extRoot, e);
        }

        return entries;
    }

    /**
     * Checks that the file is under a {@code deployment/src/main/resources/META-INF/} path.
     */
    private boolean isDeploymentResourcePath(Path file) {
        // Walk up from quarkus-skill.md: META-INF / resources / main / src / deployment
        Path current = file.getParent(); // META-INF
        if (current == null)
            return false;
        current = current.getParent(); // resources
        if (current == null || !current.getFileName().toString().equals("resources"))
            return false;
        current = current.getParent(); // main
        if (current == null || !current.getFileName().toString().equals("main"))
            return false;
        current = current.getParent(); // src
        if (current == null || !current.getFileName().toString().equals("src"))
            return false;
        current = current.getParent(); // deployment
        return current != null && current.getFileName().toString().equals(DEPLOYMENT);
    }

    /**
     * Resolves the runtime module's {@code quarkus-extension.yaml} relative to the
     * deployment module that contains the skill file.
     * <p>
     * First tries the conventional sibling {@code runtime} module. If not found,
     * scans other sibling directories for a {@code quarkus-extension.yaml} — this
     * handles extensions like Lambda (common-runtime) and other non-standard layouts.
     */
    private Path resolveRuntimeMetadata(Path skillFile) {
        // Navigate up to the deployment module directory
        // skillFile: .../extensions/{ext}/deployment/src/main/resources/META-INF/quarkus-skill.md
        Path deploymentDir = skillFile.getParent() // META-INF
                .getParent() // resources
                .getParent() // main
                .getParent() // src
                .getParent(); // deployment

        if (deploymentDir == null || !deploymentDir.getFileName().toString().equals(DEPLOYMENT)) {
            return null;
        }

        Path extensionDir = deploymentDir.getParent();
        if (extensionDir == null) {
            return null;
        }

        // Try conventional sibling "runtime" first
        Path runtimeYaml = extensionDir.resolve(RUNTIME)
                .resolve("src/main/resources")
                .resolve(SkillComposer.EXTENSION_METADATA_PATH);
        if (Files.isRegularFile(runtimeYaml)) {
            return runtimeYaml;
        }

        // Fall back: scan sibling directories for quarkus-extension.yaml
        // (handles extensions like Lambda with common-runtime, or other non-standard layouts)
        try (var siblings = Files.list(extensionDir)) {
            return siblings
                    .filter(Files::isDirectory)
                    .filter(d -> !d.getFileName().toString().equals(DEPLOYMENT))
                    .map(d -> d.resolve("src/main/resources").resolve(SkillComposer.EXTENSION_METADATA_PATH))
                    .filter(Files::isRegularFile)
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            getLog().debug("Failed to scan siblings of " + extensionDir + ": " + e.getMessage());
            return null;
        }
    }

    private void composeAndWrite(SkillEntry entry) throws IOException {
        final String rawContent = Files.readString(entry.skillFile, StandardCharsets.UTF_8);

        final ObjectNode extMeta;
        try (InputStream is = Files.newInputStream(entry.runtimeYaml)) {
            extMeta = SkillComposer.parseExtensionMetadata(is);
        }

        // Derive skill name from the runtime module's pom.xml artifactId
        Path runtimeDir = entry.runtimeYaml.getParent() // META-INF
                .getParent() // resources
                .getParent() // main
                .getParent() // src
                .getParent(); // runtime
        String skillName = readArtifactIdFromPom(runtimeDir.resolve("pom.xml"));
        Path extensionDir = runtimeDir.getParent();
        if (skillName == null) {
            // Fallback to directory name
            skillName = extensionDir != null ? extensionDir.getFileName().toString() : "unknown";
        }

        // Discover MCP tools from compiled classes and deployment source
        List<SkillComposer.McpToolInfo> mcpTools = discoverMcpTools(extensionDir);

        final String composed;
        if (!mcpTools.isEmpty()) {
            composed = SkillComposer.composeWithTools(extMeta, rawContent, skillName, mcpTools);
            getLog().debug("Composed skill: " + skillName + " with " + mcpTools.size()
                    + " MCP tools from " + entry.skillFile);
        } else {
            composed = SkillComposer.compose(extMeta, rawContent, skillName);
            getLog().debug("Composed skill: " + skillName + " from " + entry.skillFile);
        }

        final Path outputPath = outputDirectory.toPath()
                .resolve(SkillComposer.outputSkillPath(skillName));
        Files.createDirectories(outputPath.getParent());
        Files.writeString(outputPath, composed, StandardCharsets.UTF_8);
    }

    /**
     * Discovers MCP tools from an extension's modules by scanning compiled classes via Jandex:
     * <ol>
     * <li>Runtime/runtime-dev modules for {@code @JsonRpcDescription} + {@code @DevMCPEnableByDefault} methods</li>
     * <li>Deployment module for {@code @DevMcpBuildTimeTool} annotations on processor classes</li>
     * </ol>
     */
    private List<SkillComposer.McpToolInfo> discoverMcpTools(Path extensionDir) {
        List<SkillComposer.McpToolInfo> tools = new ArrayList<>();

        if (extensionDir == null || !Files.isDirectory(extensionDir)) {
            return tools;
        }

        // 1. Scan runtime-dev and runtime compiled classes via Jandex
        for (String moduleName : List.of("runtime-dev", RUNTIME)) {
            Path classesDir = extensionDir.resolve(moduleName).resolve("target/classes");
            if (Files.isDirectory(classesDir)) {
                tools.addAll(scanAnnotatedMethods(classesDir));
            }
        }

        // 2. Scan deployment compiled classes for @DevMcpBuildTimeTool annotations
        Path deploymentClasses = extensionDir.resolve(DEPLOYMENT).resolve("target/classes");
        if (Files.isDirectory(deploymentClasses)) {
            tools.addAll(scanBuildTimeToolAnnotations(deploymentClasses));
        }

        // Deduplicate by tool name (a method may be discovered from both sources)
        Map<String, SkillComposer.McpToolInfo> deduplicated = new LinkedHashMap<>();
        for (SkillComposer.McpToolInfo tool : tools) {
            deduplicated.putIfAbsent(tool.name(), tool);
        }
        return new ArrayList<>(deduplicated.values());
    }

    // ── Jandex scanning for @JsonRpcDescription methods ──────────────────────

    private static final DotName JSON_RPC_DESCRIPTION = DotName
            .createSimple("io.quarkus.runtime.annotations.JsonRpcDescription");
    private static final DotName DEV_MCP_ENABLE_BY_DEFAULT = DotName
            .createSimple("io.quarkus.runtime.annotations.DevMCPEnableByDefault");
    private static final DotName OPTIONAL = DotName.createSimple("java.util.Optional");

    /**
     * Indexes compiled classes in the given directory and extracts methods
     * annotated with both {@code @JsonRpcDescription} and {@code @DevMCPEnableByDefault}.
     * Methods without {@code @DevMCPEnableByDefault} are not enabled by default and
     * are excluded from skill files.
     */
    private List<SkillComposer.McpToolInfo> scanAnnotatedMethods(Path classesDir) {
        List<SkillComposer.McpToolInfo> tools = new ArrayList<>();

        Index index;
        try {
            index = indexClasses(classesDir);
        } catch (IOException e) {
            getLog().debug("Failed to index classes in " + classesDir + ": " + e.getMessage());
            return tools;
        }

        for (AnnotationInstance ann : index.getAnnotations(JSON_RPC_DESCRIPTION)) {
            if (ann.target().kind() != AnnotationTarget.Kind.METHOD) {
                continue;
            }

            MethodInfo method = ann.target().asMethod();

            // Skip constructors, void methods, and non-public methods
            if (method.name().equals("<init>")
                    || method.returnType().kind() == Type.Kind.VOID
                    || !java.lang.reflect.Modifier.isPublic(method.flags())) {
                continue;
            }

            // Only include methods that are enabled by default
            if (!method.hasAnnotation(DEV_MCP_ENABLE_BY_DEFAULT)) {
                continue;
            }

            AnnotationValue descValue = ann.value();
            if (descValue == null || descValue.asString().isBlank()) {
                continue;
            }

            // Extract parameter info
            Map<String, SkillComposer.ParameterInfo> params = new LinkedHashMap<>();
            for (MethodParameterInfo param : method.parameters()) {
                boolean required = !OPTIONAL.equals(param.type().name());
                String paramDesc = null;
                AnnotationInstance paramAnn = param.annotation(JSON_RPC_DESCRIPTION);
                if (paramAnn != null && paramAnn.value() != null) {
                    paramDesc = paramAnn.value().asString();
                }
                params.put(param.name(), new SkillComposer.ParameterInfo(paramDesc, required));
            }

            tools.add(new SkillComposer.McpToolInfo(method.name(), descValue.asString(), params));
        }

        return tools;
    }

    /**
     * Creates a Jandex index by scanning all {@code .class} files in the given directory.
     */
    private Index indexClasses(Path classesDir) throws IOException {
        // First try a pre-built index
        Path indexFile = classesDir.resolve("META-INF/jandex.idx");
        if (Files.isRegularFile(indexFile)) {
            try (InputStream is = Files.newInputStream(indexFile)) {
                return new IndexReader(is).read();
            } catch (Exception e) {
                getLog().debug("Failed to read existing Jandex index, re-indexing: " + e.getMessage());
            }
        }

        // Fall back to on-the-fly indexing
        Indexer indexer = new Indexer();
        try (var stream = Files.walk(classesDir)) {
            stream.filter(p -> p.toString().endsWith(".class"))
                    .forEach(p -> {
                        try (InputStream is = Files.newInputStream(p)) {
                            indexer.index(is);
                        } catch (IOException e) {
                            getLog().debug("Failed to index " + p + ": " + e.getMessage());
                        }
                    });
        }
        return indexer.complete();
    }

    // ── Jandex scanning for @DevMcpBuildTimeTool annotations on deployment classes ──

    private static final DotName DEV_MCP_BUILD_TIME_TOOL = DotName
            .createSimple("io.quarkus.devui.spi.buildtime.DevMcpBuildTimeTool");
    private static final DotName DEV_MCP_BUILD_TIME_TOOLS = DotName
            .createSimple("io.quarkus.devui.spi.buildtime.DevMcpBuildTimeTools");

    /**
     * Scans compiled deployment classes for {@code @DevMcpBuildTimeTool} annotations
     * and extracts tool metadata.
     */
    private List<SkillComposer.McpToolInfo> scanBuildTimeToolAnnotations(Path classesDir) {
        Index index;
        try {
            index = indexClasses(classesDir);
        } catch (IOException e) {
            getLog().debug("Failed to index deployment classes in " + classesDir + ": " + e.getMessage());
            return new ArrayList<>();
        }
        return scanBuildTimeToolAnnotations(index);
    }

    static List<SkillComposer.McpToolInfo> scanBuildTimeToolAnnotations(Index index) {
        List<SkillComposer.McpToolInfo> tools = new ArrayList<>();

        // Handle both single and repeatable (container) annotations
        for (AnnotationInstance ann : index.getAnnotations(DEV_MCP_BUILD_TIME_TOOL)) {
            tools.add(buildTimeToolFromAnnotation(ann));
        }
        for (AnnotationInstance container : index.getAnnotations(DEV_MCP_BUILD_TIME_TOOLS)) {
            AnnotationValue valueArray = container.value();
            if (valueArray != null) {
                for (AnnotationInstance ann : valueArray.asNestedArray()) {
                    tools.add(buildTimeToolFromAnnotation(ann));
                }
            }
        }

        return tools;
    }

    private static SkillComposer.McpToolInfo buildTimeToolFromAnnotation(AnnotationInstance ann) {
        String name = ann.value("name").asString();
        String description = ann.value("description").asString();

        Map<String, SkillComposer.ParameterInfo> params = new LinkedHashMap<>();
        AnnotationValue paramsValue = ann.value("params");
        if (paramsValue != null) {
            for (AnnotationInstance paramAnn : paramsValue.asNestedArray()) {
                String paramName = paramAnn.value("name").asString();
                AnnotationValue paramDescValue = paramAnn.value("description");
                String paramDesc = paramDescValue != null ? paramDescValue.asString() : null;
                if (paramDesc != null && paramDesc.isEmpty()) {
                    paramDesc = null;
                }
                AnnotationValue requiredValue = paramAnn.value("required");
                boolean required = requiredValue == null || requiredValue.asBoolean();
                params.put(paramName, new SkillComposer.ParameterInfo(paramDesc, required));
            }
        }

        return new SkillComposer.McpToolInfo(name, description, params.isEmpty() ? null : params);
    }

    /**
     * Reads the {@code <artifactId>} from a pom.xml file using the Maven model reader.
     */
    private String readArtifactIdFromPom(Path pomFile) {
        if (!Files.isRegularFile(pomFile)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(pomFile, StandardCharsets.UTF_8)) {
            // TODO: MavenXpp3Reader is Maven 3 specific; will need migration for Maven 4
            Model model = new MavenXpp3Reader().read(reader);
            return model.getArtifactId();
        } catch (IOException | XmlPullParserException e) {
            getLog().debug("Failed to read pom.xml: " + pomFile + ": " + e.getMessage());
            return null;
        }
    }

    private static class SkillEntry {
        final Path skillFile;
        final Path runtimeYaml;

        SkillEntry(Path skillFile, Path runtimeYaml) {
            this.skillFile = skillFile;
            this.runtimeYaml = runtimeYaml;
        }
    }
}
