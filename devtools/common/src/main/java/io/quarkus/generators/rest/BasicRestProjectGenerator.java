package io.quarkus.generators.rest;

import static java.lang.String.format;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import io.quarkus.cli.commands.writer.ProjectWriter;
import io.quarkus.generators.BuildTool;
import io.quarkus.generators.ProjectGenerator;
import io.quarkus.generators.SourceType;
import io.quarkus.maven.utilities.MojoUtils;

public class BasicRestProjectGenerator implements ProjectGenerator {

    public static final String NAME = "basic-rest";

    public BasicRestProjectGenerator() {
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void generate(final ProjectWriter writer, Map<String, Object> parameters) throws IOException {
        final BasicRestProject project = new BasicRestProject(writer, parameters);

        project.initProject();
        project.setupContext();

        project.createClasses();

        project.createIndexPage();
        project.createDockerFiles();
        project.createDockerIgnore();
        project.createApplicationConfig();

        project.createGitIgnore();
    }

    private class BasicRestProject {
        private Map<String, Object> context;
        private String path = "/hello";
        private ProjectWriter writer;
        private String srcMainPath;
        private String testMainPath;
        private SourceType type;

        private BasicRestProject(final ProjectWriter writer, final Map<String, Object> parameters) {
            this.writer = writer;
            this.context = new HashMap<>(parameters);
            this.type = (SourceType) context.get(SOURCE_TYPE);
        }

        @SuppressWarnings("unchecked")
        private <T> T get(final String key, final T defaultValue) {
            return (T) context.getOrDefault(key, defaultValue);
        }

        private boolean initProject() throws IOException {
            boolean newProject = initBuildTool();

            path = get(RESOURCE_PATH, path);

            srcMainPath = writer.mkdirs(type.getSrcDir());
            testMainPath = writer.mkdirs(type.getTestSrcDir());

            return newProject;
        }

        private boolean initBuildTool() throws IOException {
            BuildTool buildTool = get(BUILD_TOOL, BuildTool.MAVEN);
            context.putIfAbsent(ADDITIONAL_GITIGNORE_ENTRIES, buildTool.getGitIgnoreEntries());
            boolean newProject = !writer.exists(buildTool.getDependenciesFile());
            if (newProject) {
                for (String buildFile : buildTool.getBuildFiles()) {
                    generate(type.getBuildFileResourceTemplate(getName(), buildFile), context, buildFile, buildFile);
                }
            } else {
                String[] gav;
                if (BuildTool.MAVEN.equals(buildTool)) {
                    ByteArrayInputStream buildFileInputStream = new ByteArrayInputStream(
                            writer.getContent(buildTool.getDependenciesFile()));
                    gav = MojoUtils.readGavFromPom(buildFileInputStream);
                } else {
                    gav = new String[3];
                    for (String buildFile : buildTool.getBuildFiles()) {
                        if (writer.exists(buildFile)) {
                            ByteArrayInputStream buildFileInputStream = new ByteArrayInputStream(writer.getContent(buildFile));
                            gav = MojoUtils.readGavFromSettingsGradle(buildFileInputStream, gav);
                        }
                    }
                }
                context.put(PROJECT_GROUP_ID, gav[0]);
                context.put(PROJECT_ARTIFACT_ID, gav[1]);
            }
            return newProject;
        }

        private void generate(final String templateName, final Map<String, Object> context, final String outputFilePath,
                final String resourceType)
                throws IOException {
            if (!writer.exists(outputFilePath)) {
                String path = templateName.startsWith("/") ? templateName : "/" + templateName;
                URL resource = getClass().getResource(path);
                if (resource == null) {
                    throw new IOException("Template resource is missing: " + path);
                }
                try (
                        InputStream resourceStream = resource.openStream();
                        InputStreamReader streamReader = new InputStreamReader(resourceStream, StandardCharsets.UTF_8);
                        BufferedReader bufferedReader = new BufferedReader(streamReader)) {
                    String template = bufferedReader.lines().collect(Collectors.joining("\n"));
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
            String index = writer.mkdirs(resources) + "/index.html";
            if (!writer.exists(index)) {
                generate("templates/index.ftl", context, index, "welcome page");
            }

        }

        private void createDockerFiles() throws IOException {
            String dockerRoot = "src/main/docker";
            String dockerRootDir = writer.mkdirs(dockerRoot);
            generate("templates/dockerfile-native.ftl", context, dockerRootDir + "/Dockerfile.native",
                    "native docker file");
            generate("templates/dockerfile-jvm.ftl", context, dockerRootDir + "/Dockerfile.jvm", "jvm docker file");
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
            String file = writer.mkdirs(meta) + "/application.properties";
            if (!writer.exists(file)) {
                writer.write(file, "# Configuration file" + System.lineSeparator() + "# key = value");
            }
        }

        private void setupContext() throws IOException {
            if (context.get(CLASS_NAME) != null) {
                String packageName = (String) context.get(PACKAGE_NAME);

                if (packageName != null) {
                    String packageDir = srcMainPath + '/' + packageName.replace('.', '/');
                    String testPackageDir = testMainPath + '/' + packageName.replace('.', '/');
                    srcMainPath = writer.mkdirs(packageDir);
                    testMainPath = writer.mkdirs(testPackageDir);
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
                String classFile = srcMainPath + '/' + className + extension;
                String testClassFile = testMainPath + '/' + className + "Test" + extension;
                String itTestClassFile = testMainPath + '/' + "Native" + className + "IT" + extension;
                String name = getName();
                generate(type.getSrcResourceTemplate(name), context, classFile, "resource code");
                generate(type.getTestResourceTemplate(name), context, testClassFile, "test code");
                generate(type.getNativeTestResourceTemplate(name), context, itTestClassFile, "IT code");
            }
        }
    }
}
