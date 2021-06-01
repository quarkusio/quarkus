package io.quarkus.cli;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.devtools.project.codegen.CreateProjectHelper;
import picocli.CommandLine;

public class CliProjectJBangTest {
    static String startingDir;
    static Path workspaceRoot;
    Path project;

    @BeforeAll
    public static void initial() throws Exception {
        startingDir = System.getProperty("user.dir");
        workspaceRoot = Paths.get(startingDir).toAbsolutePath().resolve("target/test-project/CliProjectJBangTest");
    }

    @BeforeEach
    public void setupTestDirectories() throws Exception {
        System.setProperty("user.dir", workspaceRoot.toFile().getAbsolutePath());
        CliDriver.deleteDir(workspaceRoot);
        project = workspaceRoot.resolve("code-with-quarkus");
    }

    @AfterAll
    public static void allDone() {
        System.setProperty("user.dir", startingDir);
    }

    @Test
    public void testCreateAppDefaults() throws Exception {
        CliDriver.Result result = CliDriver.execute("create", "app", "--jbang", "--verbose", "-e", "-B");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode, "Expected OK return code." + result);
        Assertions.assertTrue(result.stdout.contains("SUCCESS"),
                "Expected confirmation that the project has been created." + result);

        Assertions.assertTrue(project.resolve("jbang").toFile().exists(),
                "Wrapper should exist by default");

        validateBasicIdentifiers(project, CreateProjectHelper.DEFAULT_GROUP_ID,
                CreateProjectHelper.DEFAULT_ARTIFACT_ID,
                CreateProjectHelper.DEFAULT_VERSION);

        Path javaMain = valdiateJBangSourcePackage(project, ""); // no package name

        String source = CliDriver.readFileAsString(javaMain);
        Assertions.assertTrue(source.contains("quarkus-resteasy"),
                "Generated source should reference resteasy. Found:\n" + source);

        System.setProperty("user.dir", project.toFile().getAbsolutePath());
        result = CliDriver.invokeValidateDryRunBuild(project);

        CliDriver.invokeValidateBuild(project);
    }

    @Test
    public void testCreateAppOverrides() throws Exception {
        project = workspaceRoot.resolve("nested/my-project");

        CliDriver.Result result = CliDriver.execute("create", "app", "--jbang", "--verbose", "-e", "-B",
                "--package-name=custom.pkg",
                "--output-directory=nested",
                "--group-id=silly", "--artifact-id=my-project", "--version=0.1.0",
                "vertx-web");

        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode, "Expected OK return code." + result);
        Assertions.assertTrue(result.stdout.contains("SUCCESS"),
                "Expected confirmation that the project has been created." + result);

        Assertions.assertTrue(project.resolve("jbang").toFile().exists(),
                "Wrapper should exist by default");

        validateBasicIdentifiers(project, "silly", "my-project", "0.1.0");
        Path javaMain = valdiateJBangSourcePackage(project, "");

        String source = CliDriver.readFileAsString(javaMain);
        Assertions.assertTrue(source.contains("quarkus-vertx-web"),
                "Generated source should reference vertx-web. Found:\n" + source);

        System.setProperty("user.dir", project.toFile().getAbsolutePath());
        result = CliDriver.invokeValidateDryRunBuild(project);

        CliDriver.invokeValidateBuild(project);
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
