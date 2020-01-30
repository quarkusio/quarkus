package io.quarkus.generators.rest;

import static java.lang.String.format;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map.Entry;
import io.quarkus.cli.commands.QuarkusCommandInvocation;
import io.quarkus.cli.commands.file.BuildFile;
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
    public void generate(final ProjectWriter writer, QuarkusCommandInvocation invocation) throws IOException {
        final BasicRestProject project = new BasicRestProject(writer, invocation);

        project.initProject();
        project.setupContext();

        project.createClasses();

        project.createIndexPage();
        project.createDockerFiles();
        project.createDockerIgnore();
        project.createApplicationConfig();
        project.createReadme();

        project.createGitIgnore();
    }

    private class BasicRestProject {
        private QuarkusCommandInvocation invocation;
        private String path = "/hello";
        private ProjectWriter writer;
        private String srcMainPath;
        private String testMainPath;
        private String nativeTestMainPath;
        private SourceType type;

        private BasicRestProject(final ProjectWriter writer, QuarkusCommandInvocation invocation) {
            this.writer = writer;
            this.invocation = invocation;
            this.type = invocation.getValue(SOURCE_TYPE, SourceType.JAVA);
        }

        @SuppressWarnings("unchecked")
        private <T> T get(final String key, final T defaultValue) {
            Object value = invocation.getValue(key);
            if(value == null) {
                value = invocation.getProperty(key);
            }
            return value == null ? defaultValue : (T) value;
        }

        private boolean initProject() throws IOException {
            boolean newProject = initBuildTool();

            path = get(RESOURCE_PATH, path);

            srcMainPath = writer.mkdirs(type.getSrcDir());
            testMainPath = writer.mkdirs(type.getTestSrcDir());
            // for gradle we want to place the native tests in under 'src/native-test/java'
            // since gradle's idiomatic way of running integration tests is to create a new source set
            if (getBuildTool() == BuildTool.GRADLE) {
                nativeTestMainPath = writer.mkdirs(type.getTestSrcDir().replace("test", "native-test"));
            } else {
                nativeTestMainPath = testMainPath;
            }

            return newProject;
        }

        private boolean initBuildTool() throws IOException {
            BuildTool buildTool = getBuildTool();
            if (!invocation.hasProperty(ADDITIONAL_GITIGNORE_ENTRIES)) {
                invocation.setProperty(ADDITIONAL_GITIGNORE_ENTRIES, buildTool.getGitIgnoreEntries());
            }
            if (!invocation.hasProperty(BUILD_DIRECTORY)) {
                invocation.setProperty(BUILD_DIRECTORY, buildTool.getBuildDirectory());
            }

            boolean newProject = !writer.exists(buildTool.getDependenciesFile());
            if (newProject) {
                for (String buildFile : buildTool.getBuildFiles()) {
                    generate(type.getBuildFileResourceTemplate(getName(), buildFile), invocation, buildFile, buildFile);
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
                            try (ByteArrayInputStream buildFileInputStream = new ByteArrayInputStream(
                                    writer.getContent(buildFile))) {
                                gav = MojoUtils.readGavFromSettingsGradle(buildFileInputStream, gav);
                            }
                        }
                    }
                }
                if (gav[0] != null) {
                    invocation.setProperty(PROJECT_GROUP_ID, gav[0]);
                }
                if (gav[1] != null) {
                    invocation.setProperty(PROJECT_ARTIFACT_ID, gav[1]);
                }
            }
            return newProject;
        }

        private BuildTool getBuildTool() {
            BuildFile buildFileManager = get(BUILD_FILE, null);
            return buildFileManager == null ? BuildTool.MAVEN : buildFileManager.getBuildTool();
        }

        private void generate(final String templateName, QuarkusCommandInvocation invocation, final String outputFilePath,
                final String resourceType)
                throws IOException {
            if (!writer.exists(outputFilePath)) {
                String template = invocation.getPlatformDescriptor().getTemplate(templateName);
                if (template == null) {
                    throw new IOException("Template resource is missing: " + templateName);
                }
                for (Entry<Object, Object> e : invocation.getProperties().entrySet()) {
                    if (e.getValue() != null) { // Exclude null values (classname and path can be null)
                        template = template.replace(format("${%s}", e.getKey().toString()), e.getValue().toString());
                    }
                }
                writer.write(outputFilePath, template);
            }
        }

        private void createIndexPage() throws IOException {
            // Generate index page
            String resources = "src/main/resources/META-INF/resources";
            String index = writer.mkdirs(resources) + "/index.html";
            if (!writer.exists(index)) {
                generate("templates/index.ftl", invocation, index, "welcome page");
            }

        }

        private void createDockerFiles() throws IOException {
            String dockerRoot = "src/main/docker";
            String dockerRootDir = writer.mkdirs(dockerRoot);
            generate("templates/dockerfile-native.ftl", invocation, dockerRootDir + "/Dockerfile.native",
                    "native docker file");
            generate("templates/dockerfile-jvm.ftl", invocation, dockerRootDir + "/Dockerfile.jvm", "jvm docker file");
        }

        private void createReadme() throws IOException {
            String readme = writer.mkdirs("") + "README.md";
            BuildTool buildTool = getBuildTool();
            switch (buildTool) {
                case MAVEN:
                    generate("templates/README.maven.ftl", invocation, readme,"read me");
                    break;
                case GRADLE:
                    generate("templates/README.gradle.ftl", invocation, readme, "read me");
                    break;
                default:
                    throw new IllegalStateException("buildTool is none of Maven or Gradle");
            }
        }

        private void createDockerIgnore() throws IOException {
            String docker = writer.mkdirs("") + ".dockerignore";
            generate("templates/dockerignore.ftl", invocation, docker, "docker ignore");
        }

        private void createGitIgnore() throws IOException {
            String gitignore = writer.mkdirs("") + ".gitignore";
            generate("templates/gitignore.ftl", invocation, gitignore, "git ignore");
        }

        private void createApplicationConfig() throws IOException {
            String meta = "src/main/resources";
            String file = writer.mkdirs(meta) + "/application.properties";
            if (!writer.exists(file)) {
                writer.write(file, "# Configuration file" + System.lineSeparator() + "# key = value");
            }
        }

        private void setupContext() throws IOException {
            if (invocation.getProperty(CLASS_NAME) != null) {
                String packageName = invocation.getProperty(PACKAGE_NAME);

                if (packageName != null) {
                    String packageDir = srcMainPath + '/' + packageName.replace('.', '/');
                    String originalTestMainPath = testMainPath;
                    String testPackageDir = testMainPath + '/' + packageName.replace('.', '/');
                    srcMainPath = writer.mkdirs(packageDir);
                    testMainPath = writer.mkdirs(testPackageDir);

                    if (!originalTestMainPath.equals(nativeTestMainPath)) {
                        String nativeTestPackageDir = nativeTestMainPath + '/' + packageName.replace('.', '/');
                        nativeTestMainPath = writer.mkdirs(nativeTestPackageDir);
                    } else {
                        nativeTestMainPath = testMainPath;
                    }

                } else {
                    throw new NullPointerException("Need a non-null package name");
                }
            }
        }

        private void createClasses() throws IOException {
            Object className = invocation.getProperty(CLASS_NAME);
            // If className is null we disable the generation of the JAX-RS resource.
            if (className != null) {
                String extension = type.getExtension();
                String classFile = srcMainPath + '/' + className + extension;
                String testClassFile = testMainPath + '/' + className + "Test" + extension;
                String itTestClassFile = nativeTestMainPath + '/' + "Native" + className + "IT" + extension;
                String name = getName();
                String srcResourceTemplate = type.getSrcResourceTemplate(name);
                Object isSpring = invocation.getValue(IS_SPRING);
                if (isSpring != null && (Boolean) invocation.getValue(IS_SPRING).equals(Boolean.TRUE)) {
                    srcResourceTemplate = type.getSrcSpringControllerTemplate(name);
                }
                generate(srcResourceTemplate, invocation, classFile, "resource code");
                generate(type.getTestResourceTemplate(name), invocation, testClassFile, "test code");
                generate(type.getNativeTestResourceTemplate(name), invocation, itTestClassFile, "IT code");
            }
        }
    }
}
