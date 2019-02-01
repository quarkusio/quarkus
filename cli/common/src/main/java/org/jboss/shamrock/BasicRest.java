package org.jboss.shamrock;

import freemarker.template.TemplateException;
import org.apache.maven.model.Model;
import org.jboss.shamrock.maven.utilities.MojoUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Map;

public class BasicRest extends ShamrockTemplate {
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
        createDockerFile();
        createMicroProfileConfig();
    }

    private void setupContext() {
        MojoUtils.getAllProperties().forEach((k, v) -> context.put(k.replace("-", "_"), v));

        if (className != null) {
            String packageName = (String) context.get(PACKAGE_NAME);

            if (className.endsWith(MojoUtils.JAVA_EXTENSION)) {
                className = className.substring(0, className.length() - MojoUtils.JAVA_EXTENSION.length());
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
        File classFile = new File(srcMain, className + MojoUtils.JAVA_EXTENSION);
        File testClassFile = new File(testMain, className + "Test" + MojoUtils.JAVA_EXTENSION);
        generate("templates/resource-template.ftl", context, classFile, "resource code");
        generate("templates/test-resource-template.ftl", context, testClassFile, "test code");
    }

    @SuppressWarnings("unchecked")
    private <T> T get(final String key, final String defaultValue) {
        return (T) context.getOrDefault(key, defaultValue);
    }

    private boolean initProject() throws IOException {
        final File pomFile = new File(projectRoot, "pom.xml");
        boolean newProject = !pomFile.exists();
        if (newProject) {
            generate("templates/pom-template.ftl", context, pomFile, "Unable to generate pom.xml");
        } else {
            final Model model = MojoUtils.readPom(pomFile);
            context.put(PROJECT_GROUP_ID, model.getGroupId());
            context.put(PROJECT_ARTIFACT_ID, model.getArtifactId());
        }

        // If className is null we disable the generation of the Jax-RS resource.
        className = get("className", null);
        path = get(RESOURCE_PATH, path);

        srcMain = mkdirs(new File(projectRoot, "src/main/java"));
        testMain = mkdirs(new File(projectRoot, "src/test/java"));

        return newProject;
    }

    private void generate(final String templateName, final Map<String, Object> context, final File outputFile, final String resourceType)
        throws IOException {
        if (!outputFile.exists()) {
            try (Writer out = new FileWriter(outputFile)) {
                cfg.getTemplate(templateName)
                   .process(context, out);
            } catch (TemplateException e) {
                throw new RuntimeException("Unable to generate " + resourceType, e);
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

    private void createDockerFile() throws IOException {
        File dockerRoot = new File(projectRoot, "src/main/docker");
        File docker = new File(mkdirs(dockerRoot), "Dockerfile");
        generate("templates/dockerfile.ftl", context, docker, "docker file");
    }

    private void createMicroProfileConfig() throws IOException {
        File meta = new File(projectRoot, "src/main/resources/META-INF");
        File file = new File(mkdirs(meta), "microprofile-config.properties");
        if (!file.exists()) {
            Files.write(file.toPath(), Arrays.asList("# Configuration file", "key = value"), StandardOpenOption.CREATE_NEW);
            System.out.println("Configuration file created in src/main/resources/META-INF/" + file.getName());
        }
    }

    private File mkdirs(File dir) {
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }
}
