package io.quarkus;

import static java.lang.String.format;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.maven.model.Model;

import io.quarkus.maven.utilities.MojoUtils;

public class BasicRest extends QuarkusTemplate {
    private Map<String, Object> context;
    private String className;
    private String path = "/hello";
    private File projectRoot;
    private File srcMain;
    private File testMain;

    @Override
    public String getName() {
        return "basic-rest";
    }

    @Override
    public void generate(final File projectRoot, Map<String, Object> parameters) throws IOException {
        this.projectRoot = projectRoot;
        this.context = parameters;

        initProject();
        setupContext();

        if (className != null) {
            createClasses();
        }
        createIndexPage();
        createDockerFiles();
        createDockerIgnore();
        createApplicationConfig();
    }

    private void setupContext() {
        MojoUtils.getAllProperties().forEach((k, v) -> context.put(k.replace("-", "_"), v));

        if (className != null) {
            String packageName = (String) context.get(PACKAGE_NAME);

            if (className.endsWith(MojoUtils.JAVA_EXTENSION)) {
                className = className.substring(0, className.length() - MojoUtils.JAVA_EXTENSION.length());
            } else if (className.endsWith(MojoUtils.KOTLIN_EXTENSION)) {
                className = className.substring(0, className.length() - MojoUtils.KOTLIN_EXTENSION.length());
            }

            if (className.contains(".")) {
                int idx = className.lastIndexOf('.');
                packageName = className.substring(0, idx);
                className = className.substring(idx + 1);
            }

            if (packageName != null) {
                File packageDir = new File(srcMain, packageName.replace('.', '/'));
                File testPackageDir = new File(testMain, packageName.replace('.', '/'));
                srcMain = mkdirs(packageDir);
                testMain = mkdirs(testPackageDir);
            }

            context.put(CLASS_NAME, className);
            context.put(RESOURCE_PATH, path);

            if (packageName != null) {
                context.put(PACKAGE_NAME, packageName);
            }
        }
    }

    private void createClasses() throws IOException {
        File classFile = new File(srcMain, className + getProperSourceExtension());
        File testClassFile = new File(testMain, className + "Test" + getProperSourceExtension());
        File itTestClassFile = new File(testMain, "Native" + className + "IT" + getProperSourceExtension());
        generate(
                getSourceType() == SourceType.JAVA
                        ? "templates/resource-template.ftl"
                        : "templates/resource-template-kotlin.ftl",
                context, classFile, "resource code");
        generate(
                getSourceType() == SourceType.JAVA
                        ? "templates/test-resource-template.ftl"
                        : "templates/test-resource-template-kotlin.ftl",
                context, testClassFile, "test code");
        generate(
                getSourceType() == SourceType.JAVA
                        ? "templates/native-test-resource-template.ftl"
                        : "templates/native-test-resource-template-kotlin.ftl",
                context, itTestClassFile, "IT code");
    }

    @SuppressWarnings("unchecked")
    private <T> T get(final String key, final String defaultValue) {
        return (T) context.getOrDefault(key, defaultValue);
    }

    private boolean initProject() throws IOException {
        final File pomFile = new File(projectRoot, "pom.xml");
        boolean newProject = !pomFile.exists();
        if (newProject) {
            final String templateName = getSourceType() == SourceType.JAVA ? "templates/pom-template.ftl"
                    : "templates/pom-template-kotlin.ftl";
            generate(templateName, context, pomFile, "Unable to generate pom.xml");
        } else {
            final Model model = MojoUtils.readPom(pomFile);
            context.put(PROJECT_GROUP_ID, model.getGroupId());
            context.put(PROJECT_ARTIFACT_ID, model.getArtifactId());
        }

        // If className is null we disable the generation of the Jax-RS resource.
        className = get("className", null);
        path = get(RESOURCE_PATH, path);

        srcMain = mkdirs(new File(projectRoot, getSourceType() == SourceType.JAVA ? "src/main/java" : "src/main/kotlin"));
        testMain = mkdirs(new File(projectRoot, getSourceType() == SourceType.JAVA ? "src/test/java" : "src/test/kotlin"));

        return newProject;
    }

    private void generate(final String templateName, final Map<String, Object> context, final File outputFile,
            final String resourceType)
            throws IOException {
        if (!outputFile.exists()) {
            String path = templateName.startsWith("/") ? templateName : "/" + templateName;
            try (BufferedWriter out = Files.newBufferedWriter(outputFile.toPath());
                    final BufferedReader stream = new BufferedReader(
                            new InputStreamReader(getClass().getResourceAsStream(path), StandardCharsets.UTF_8))) {
                String template = stream.lines().collect(Collectors.joining("\n"));
                for (Entry<String, Object> e : context.entrySet()) {
                    if (e.getValue() != null) { // Exclude null values (classname and path can be null)
                        template = template.replace(format("${%s}", e.getKey()), e.getValue().toString());
                    }
                }
                out.write(template);
            }
        }
    }

    private void createIndexPage() throws IOException {
        // Generate index page
        File resources = new File(projectRoot, "src/main/resources/META-INF/resources");
        File index = new File(mkdirs(resources), "index.html");
        if (!index.exists()) {
            generate("templates/index.ftl", context, index, "welcome page");
        }

    }

    private void createDockerFiles() throws IOException {
        File dockerRoot = new File(projectRoot, "src/main/docker");
        generate("templates/dockerfile-native.ftl", context, new File(mkdirs(dockerRoot), "Dockerfile.native"),
                "native docker file");
        generate("templates/dockerfile-jvm.ftl", context, new File(mkdirs(dockerRoot), "Dockerfile.jvm"), "jvm docker file");
    }

    private void createDockerIgnore() throws IOException {
        File dockerRoot = new File(projectRoot, "");
        File docker = new File(mkdirs(dockerRoot), ".dockerignore");
        generate("templates/dockerignore.ftl", context, docker, "docker ignore");
    }

    private void createApplicationConfig() throws IOException {
        File meta = new File(projectRoot, "src/main/resources");
        File file = new File(mkdirs(meta), "application.properties");
        if (!file.exists()) {
            Files.write(file.toPath(), Arrays.asList("# Configuration file", "# key = value"), StandardOpenOption.CREATE_NEW);
            System.out.println("Configuration file created in src/main/resources/META-INF/" + file.getName());
        }
    }

    private File mkdirs(File dir) {
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    private SourceType getSourceType() {
        return (SourceType) context.get(SOURCE_TYPE);
    }

    private String getProperSourceExtension() {
        return getSourceType() == SourceType.JAVA ? MojoUtils.JAVA_EXTENSION : MojoUtils.KOTLIN_EXTENSION;
    }
}
