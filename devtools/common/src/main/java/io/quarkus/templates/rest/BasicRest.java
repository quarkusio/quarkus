package io.quarkus.templates.rest;

import static java.lang.String.format;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.maven.model.Model;

import io.quarkus.cli.commands.writer.Writer;
import io.quarkus.maven.utilities.MojoUtils;
import io.quarkus.templates.QuarkusTemplate;
import io.quarkus.templates.SourceType;

public class BasicRest implements QuarkusTemplate {

    public static final String TEMPLATE_NAME = "basic-rest";

    private Map<String, Object> context;
    private String path = "/hello";
    private Writer writer;
    private String srcMain;
    private String testMain;
    private SourceType type;

    public BasicRest() {
    }

    @Override
    public String getName() {
        return TEMPLATE_NAME;
    }

    @Override
    public void generate(final Writer writer, Map<String, Object> parameters) throws IOException {
        this.writer = writer;
        this.context = parameters == null ? Collections.emptyMap() : parameters;
        this.type = (SourceType) context.get(SOURCE_TYPE);

        initProject();
        setupContext();

        createClasses();

        createIndexPage();
        createDockerFiles();
        createDockerIgnore();
        createApplicationConfig();

        createGitIgnore();
    }

    private void setupContext() throws IOException {
        if (context.get(CLASS_NAME) != null) {
            String packageName = (String) context.get(PACKAGE_NAME);

            if (packageName != null) {
                String packageDir = srcMain + '/' + packageName.replace('.', '/');
                String testPackageDir = testMain + '/' + packageName.replace('.', '/');
                srcMain = writer.mkdirs(packageDir);
                testMain = writer.mkdirs(testPackageDir);
            } else {
                throw new NullPointerException("Need a non-null package name");
            }
        }
    }

    private void createClasses() throws IOException {
        Object className = context.get(CLASS_NAME);
        // If className is null we disable the generation of the JAX-RS resource.
        if (className != null) {
            String extension = type.getExtension();
            String classFile = srcMain + '/' + className + extension;
            String testClassFile = testMain + '/' + className + "Test" + extension;
            String itTestClassFile = testMain + '/' + "Native" + className + "IT" + extension;
            String name = getName();
            generate(type.getSrcResourceTemplate(name), context, classFile, "resource code");
            generate(type.getTestResourceTemplate(name), context, testClassFile, "test code");
            generate(type.getNativeTestResourceTemplate(name), context, itTestClassFile, "IT code");
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T get(final String key, final String defaultValue) {
        return (T) context.getOrDefault(key, defaultValue);
    }

    private boolean initProject() throws IOException {
        boolean newProject = !writer.exists("pom.xml");
        if (newProject) {
            generate(type.getPomResourceTemplate(getName()), context, "pom.xml", "pom.xml");
        } else {
            final Model model = MojoUtils.readPom(new ByteArrayInputStream(writer.getContent("pom.xml")));
            context.put(PROJECT_GROUP_ID, model.getGroupId());
            context.put(PROJECT_ARTIFACT_ID, model.getArtifactId());
        }

        path = get(RESOURCE_PATH, path);

        srcMain = writer.mkdirs(type.getSrcDir());
        testMain = writer.mkdirs(type.getTestSrcDir());

        return newProject;
    }

    private void generate(final String templateName, final Map<String, Object> context, final String outputFilePath,
            final String resourceType)
            throws IOException {
        if (!writer.exists(outputFilePath)) {
            String path = templateName.startsWith("/") ? templateName : "/" + templateName;
            try (final BufferedReader stream = new BufferedReader(
                    new InputStreamReader(getClass().getResourceAsStream(path), StandardCharsets.UTF_8))) {
                String template = stream.lines().collect(Collectors.joining("\n"));
                for (Entry<String, Object> e : context.entrySet()) {
                    if (e.getValue() != null) { // Exclude null values (classname and path can be null)
                        template = template.replace(format("${%s}", e.getKey()), e.getValue().toString());
                    }
                }
                writer.write(outputFilePath, template);
            }
        }
    }

    private void createIndexPage() throws IOException {
        // Generate index page
        String resources = "src/main/resources/META-INF/resources";
        String index = writer.mkdirs(resources) + "index.html";
        if (!writer.exists(index)) {
            generate("templates/index.ftl", context, index, "welcome page");
        }

    }

    private void createDockerFiles() throws IOException {
        String dockerRoot = "src/main/docker";
        generate("templates/dockerfile-native.ftl", context, writer.mkdirs(dockerRoot) + "Dockerfile.native",
                "native docker file");
        generate("templates/dockerfile-jvm.ftl", context, writer.mkdirs(dockerRoot) + "Dockerfile.jvm", "jvm docker file");
    }

    private void createDockerIgnore() throws IOException {
        String docker = writer.mkdirs("") + ".dockerignore";
        generate("templates/dockerignore.ftl", context, docker, "docker ignore");
    }

    private void createGitIgnore() throws IOException {
        String gitignore = writer.mkdirs("") + ".gitignore";
        generate("templates/gitignore.ftl", context, gitignore, "git ignore");
    }

    private void createApplicationConfig() throws IOException {
        String meta = "src/main/resources";
        String file = writer.mkdirs(meta) + "application.properties";
        if (!writer.exists(file)) {
            writer.write(file, "# Configuration file" + System.lineSeparator() + "# key = value");
            System.out.println("Configuration file created in " + file);
        }
    }
}
