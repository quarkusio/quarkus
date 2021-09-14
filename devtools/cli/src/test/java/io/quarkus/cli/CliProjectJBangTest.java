package io.quarkus.cli;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.devtools.project.codegen.CreateProjectHelper;
import picocli.CommandLine;

public class CliProjectJBangTest {
    static Path workspaceRoot = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
            .resolve("target/test-project/CliProjectJBangTest");

    Path project;

    @BeforeEach
    public void setupTestDirectories() throws Exception {
        CliDriver.deleteDir(workspaceRoot);
        project = workspaceRoot.resolve("code-with-quarkus");
    }

    @Test
    public void testCreateAppDefaults() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "create", "app", "--jbang", "--verbose", "-e", "-B");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode, "Expected OK return code." + result);
        Assertions.assertTrue(result.stdout.contains("SUCCESS"),
                "Expected confirmation that the project has been created." + result);

        Assertions.assertTrue(project.resolve("jbang").toFile().exists(),
                "Wrapper should exist by default");

        validateBasicIdentifiers(project, CreateProjectHelper.DEFAULT_GROUP_ID,
                CreateProjectHelper.DEFAULT_ARTIFACT_ID,
                CreateProjectHelper.DEFAULT_VERSION);

        Path javaMain = valdiateJBangSourcePackage(project, ""); // no package name

        String source = CliDriver.readFileAsString(project, javaMain);
        Assertions.assertTrue(source.contains("quarkus-resteasy"),
                "Generated source should reference resteasy. Found:\n" + source);

        result = CliDriver.invokeValidateDryRunBuild(project);

        CliDriver.invokeValidateBuild(project);
    }

    @Test
    public void testCreateAppOverrides() throws Exception {
        Path nested = workspaceRoot.resolve("cli-nested");
        project = nested.resolve("my-project");

        CliDriver.Result result = CliDriver.execute(workspaceRoot, "create", "app", "--jbang", "--verbose", "-e", "-B",
                "--package-name=custom.pkg",
                "--output-directory=" + nested,
                "--group-id=silly", "--artifact-id=my-project", "--version=0.1.0",
                "vertx-web");

        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode, "Expected OK return code." + result);
        Assertions.assertTrue(result.stdout.contains("SUCCESS"),
                "Expected confirmation that the project has been created." + result);

        Assertions.assertTrue(project.resolve("jbang").toFile().exists(),
                "Wrapper should exist by default");

        validateBasicIdentifiers(project, "silly", "my-project", "0.1.0");
        Path javaMain = valdiateJBangSourcePackage(project, "");

        String source = CliDriver.readFileAsString(project, javaMain);
        Assertions.assertTrue(source.contains("quarkus-vertx-web"),
                "Generated source should reference vertx-web. Found:\n" + source);

        result = CliDriver.invokeValidateDryRunBuild(project);

        CliDriver.invokeValidateBuild(project);
    }

    @Test
    public void testCreateCliDefaults() throws Exception {
        CliDriver.Result result = CliDriver.execute(workspaceRoot, "create", "cli", "--jbang", "--verbose", "-e", "-B");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode, "Expected OK return code." + result);
        Assertions.assertTrue(result.stdout.contains("SUCCESS"),
                "Expected confirmation that the project has been created." + result);

        Assertions.assertTrue(project.resolve("jbang").toFile().exists(),
                "Wrapper should exist by default");

        validateBasicIdentifiers(project, CreateProjectHelper.DEFAULT_GROUP_ID,
                CreateProjectHelper.DEFAULT_ARTIFACT_ID,
                CreateProjectHelper.DEFAULT_VERSION);

        Path javaMain = valdiateJBangSourcePackage(project, ""); // no package name

        String source = CliDriver.readFileAsString(project, javaMain);
        Assertions.assertFalse(source.contains("quarkus-resteasy"),
                "Generated source should reference resteasy. Found:\n" + source);
        Assertions.assertTrue(source.contains("quarkus-picocli"),
                "Generated source should not reference picocli. Found:\n" + source);

        result = CliDriver.invokeValidateDryRunBuild(project);

        result = CliDriver.execute(project, "build", "-e", "-B", "--clean", "--verbose",
                "-Dproperty=value1", "-Dproperty2=value2");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code. Result:\n" + result);
    }

    void validateBasicIdentifiers(Path project, String group, String artifact, String version) throws Exception {
        Assertions.assertTrue(project.resolve("README.md").toFile().exists(),
                "README.md should exist: " + project.resolve("README.md").toAbsolutePath().toString());

        // TODO: while jbang supports packages, the generation command does not
        // Should the version be stored in metadata anywhere? Is that useful for script management?
    }

    Path valdiateJBangSourcePackage(Path project, String name) {
        Path packagePath = project.resolve("src/" + name);
        Assertions.assertTrue(packagePath.toFile().exists(),
                "Package directory should exist: " + packagePath.toAbsolutePath().toString());
        Assertions.assertTrue(packagePath.toFile().isDirectory(),
                "Package directory should be a directory: " + packagePath.toAbsolutePath().toString());

        return packagePath.resolve("main.java");
    }
}
