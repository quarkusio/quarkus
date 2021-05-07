package io.quarkus.cli;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.quarkus.cli.core.ExecuteUtil;
import io.quarkus.devtools.testing.RegistryClientTestHelper;
import picocli.CommandLine;

public class CliTest {

    private String screen;
    private int exitCode;

    private String cwd;
    private Path workspace;

    @BeforeAll
    public static void globalSetup() {
        RegistryClientTestHelper.enableRegistryClientTestConfig();
    }

    @AfterAll
    public static void globalReset() {
        RegistryClientTestHelper.disableRegistryClientTestConfig();
    }

    @BeforeEach
    public void setupTestDirectories() throws Exception {
        cwd = System.getProperty("user.dir");
        workspace = Paths.get(cwd).toAbsolutePath().resolve("target/cli-workspace");
        deleteDir(workspace);
        Assertions.assertFalse(workspace.toFile().exists());
        Files.createDirectories(workspace);
        System.setProperty("user.dir", workspace.toFile().getAbsolutePath());
    }

    @AfterEach
    public void revertCWD() {
        System.setProperty("user.dir", cwd);
    }

    @Test
    public void testCreateMavenDefaults() throws Exception {
        Path project = workspace.resolve("code-with-quarkus");

        execute("create");

        Assertions.assertEquals(CommandLine.ExitCode.OK, exitCode);
        Assertions.assertTrue(screen.contains("Project code-with-quarkus created"));

        Assertions.assertTrue(project.resolve("pom.xml").toFile().exists());
        String pom = readString(project.resolve("pom.xml"));
        Assertions.assertTrue(pom.contains("<groupId>org.acme</groupId>"));
        Assertions.assertTrue(pom.contains("<artifactId>code-with-quarkus</artifactId>"));
        Assertions.assertTrue(pom.contains("<version>1.0.0-SNAPSHOT</version>"));
    }

    @Test
    public void testAddListRemove() throws Exception {
        Path project = workspace.resolve("code-with-quarkus");
        testCreateMavenDefaults();

        execute("add");
        Assertions.assertEquals(CommandLine.ExitCode.USAGE, exitCode);

        // test not project dir
        execute("add", "qute");
        Assertions.assertEquals(CommandLine.ExitCode.SOFTWARE, exitCode);

        execute("list");
        Assertions.assertEquals(CommandLine.ExitCode.SOFTWARE, exitCode);

        System.out.println("Change user.dir to: " + project.toFile().getAbsolutePath());
        System.setProperty("user.dir", project.toFile().getAbsolutePath());

        // test empty list
        execute("list");
        Assertions.assertEquals(CommandLine.ExitCode.OK, exitCode);
        Assertions.assertEquals("quarkus-resteasy", screen.trim());

        // test add
        execute("add", "qute");
        Assertions.assertEquals(CommandLine.ExitCode.OK, exitCode);

        // test list

        execute("list");
        Assertions.assertEquals(CommandLine.ExitCode.OK, exitCode);
        Assertions.assertTrue(screen.contains("quarkus-resteasy"));
        Assertions.assertTrue(screen.contains("quarkus-qute"));

        // test add multiple
        execute("add", "amazon-lambda-http", "jackson");
        Assertions.assertEquals(CommandLine.ExitCode.OK, exitCode);

        // test list

        execute("list");
        Assertions.assertEquals(CommandLine.ExitCode.OK, exitCode);
        Assertions.assertTrue(screen.contains("quarkus-resteasy"));
        Assertions.assertTrue(screen.contains("quarkus-qute"));
        Assertions.assertTrue(screen.contains("quarkus-amazon-lambda-http"));
        Assertions.assertTrue(screen.contains("quarkus-jackson"));

        // test list installable
        execute("list", "--installable");
        Assertions.assertEquals(CommandLine.ExitCode.OK, exitCode);
        Assertions.assertFalse(screen.contains("quarkus-jackson"));
        Assertions.assertTrue(screen.contains("quarkus-azure-functions-http"));

        // test list search installable
        execute("list", "--installable", "--search=picocli");
        Assertions.assertEquals(CommandLine.ExitCode.OK, exitCode);
        Assertions.assertEquals("quarkus-picocli", screen.trim());

        // test list search
        execute("list", "--search=amazon");
        Assertions.assertEquals(CommandLine.ExitCode.OK, exitCode);
        Assertions.assertEquals("quarkus-amazon-lambda-http", screen.trim());

        // test remove bad
        execute("remove", "badbadbad");
        Assertions.assertEquals(CommandLine.ExitCode.SOFTWARE, exitCode);

        // test remove
        execute("remove", "qute");
        Assertions.assertEquals(CommandLine.ExitCode.OK, exitCode);
        execute("list");
        Assertions.assertEquals(CommandLine.ExitCode.OK, exitCode);
        Assertions.assertFalse(screen.contains("quarkus-qute"));
        Assertions.assertTrue(screen.contains("quarkus-amazon-lambda-http"));
        Assertions.assertTrue(screen.contains("quarkus-jackson"));

        // test remove many
        execute("rm", "amazon-lambda-http", "jackson", "resteasy");
        Assertions.assertEquals(CommandLine.ExitCode.OK, exitCode);
        execute("list");
        Assertions.assertEquals(CommandLine.ExitCode.OK, exitCode);
        Assertions.assertEquals("", screen.trim());

    }

    @Test
    @Tag("failsOnJDK16")
    public void testGradleAddListRemove() throws Exception {
        // Gradle list command cannot be screen captured with the current implementation
        // so I will just test good return values
        //
        Path project = workspace.resolve("code-with-quarkus");
        testCreateGradleDefaults();

        execute("add");
        Assertions.assertEquals(CommandLine.ExitCode.USAGE, exitCode);

        // test not project dir
        execute("add", "resteasy");
        Assertions.assertEquals(CommandLine.ExitCode.SOFTWARE, exitCode);

        execute("list");
        Assertions.assertEquals(CommandLine.ExitCode.SOFTWARE, exitCode);

        System.out.println("Change user.dir to: " + project.toFile().getAbsolutePath());
        System.setProperty("user.dir", project.toFile().getAbsolutePath());

        // test empty list
        execute("list");
        Assertions.assertEquals(CommandLine.ExitCode.OK, exitCode);

        // test add
        execute("add", "resteasy");
        Assertions.assertEquals(CommandLine.ExitCode.OK, exitCode);

        // test list

        execute("list");
        Assertions.assertEquals(CommandLine.ExitCode.OK, exitCode);
        Assertions.assertTrue(screen.contains("quarkus-resteasy"));
        Assertions.assertFalse(screen.contains("quarkus-amazon-lambda-http"));

        // test add multiple
        execute("add", "amazon-lambda-http", "jackson");
        Assertions.assertEquals(CommandLine.ExitCode.OK, exitCode);

        // test list

        execute("list");
        Assertions.assertEquals(CommandLine.ExitCode.OK, exitCode);
        Assertions.assertTrue(screen.contains("quarkus-resteasy"));
        Assertions.assertTrue(screen.contains("quarkus-amazon-lambda-http"));
        Assertions.assertTrue(screen.contains("quarkus-jackson"));

        // test list installable
        execute("list", "--installable");
        Assertions.assertEquals(CommandLine.ExitCode.OK, exitCode);
        Assertions.assertFalse(screen.contains("quarkus-jackson"));
        Assertions.assertTrue(screen.contains("quarkus-azure-functions-http"));

        // test list search installable
        execute("list", "--installable", "--search=picocli");
        Assertions.assertEquals(CommandLine.ExitCode.OK, exitCode);
        Assertions.assertTrue(screen.contains("quarkus-picocli"));
        Assertions.assertFalse(screen.contains("quarkus-amazon-lambda-http"));

        // test list search
        execute("list", "--search=amazon");
        Assertions.assertEquals(CommandLine.ExitCode.OK, exitCode);
        Assertions.assertFalse(screen.contains("quarkus-picocli"));
        Assertions.assertTrue(screen.contains("quarkus-amazon-lambda-http"));

        // test remove
        execute("remove", "resteasy");
        Assertions.assertEquals(CommandLine.ExitCode.OK, exitCode);
        execute("list");
        Assertions.assertEquals(CommandLine.ExitCode.OK, exitCode);
        Assertions.assertFalse(screen.contains("quarkus-resteasy"));
        Assertions.assertTrue(screen.contains("quarkus-amazon-lambda-http"));
        Assertions.assertTrue(screen.contains("quarkus-jackson"));

        // test remove many
        execute("rm", "amazon-lambda-http", "jackson");
        Assertions.assertEquals(CommandLine.ExitCode.OK, exitCode);
        execute("list");
        Assertions.assertEquals(CommandLine.ExitCode.OK, exitCode);
        Assertions.assertFalse(screen.contains("quarkus-resteasy"));
        Assertions.assertFalse(screen.contains("quarkus-amazon-lambda-http"));
        Assertions.assertFalse(screen.contains("quarkus-jackson"));

    }

    @Test
    public void testCreateMaven() throws Exception {
        Path project = workspace.resolve("my-rest-project");

        execute("create",
                "--group-id=com.acme",
                "--artifact-id=my-rest-project",
                "--version=4.2",
                "--maven",
                "resteasy");

        Assertions.assertEquals(0, exitCode);
        Assertions.assertTrue(screen.contains("Project my-rest-project created"));

        Assertions.assertTrue(project.resolve("pom.xml").toFile().exists());
        String pom = readString(project.resolve("pom.xml"));
        Assertions.assertTrue(pom.contains("<groupId>com.acme</groupId>"));
        Assertions.assertTrue(pom.contains("<artifactId>my-rest-project</artifactId>"));
        Assertions.assertTrue(pom.contains("<version>4.2</version>"));
        Assertions.assertTrue(pom.contains("<artifactId>quarkus-resteasy</artifactId>"));
    }

    @Test
    public void testCreateWithAppConfig() throws Exception {
        Path project = workspace.resolve("code-with-quarkus");

        List<String> configs = Arrays.asList("custom.app.config1=val1",
                "custom.app.config2=val2", "lib.config=val3");

        execute("create", "--app-config=" + StringUtils.join(configs, ","));

        Assertions.assertEquals(CommandLine.ExitCode.OK, exitCode);
        Assertions.assertTrue(screen.contains("Project code-with-quarkus created"));

        Assertions.assertTrue(project.resolve("src/main/resources/application.properties").toFile().exists());
        String propertiesFile = readString(project.resolve("src/main/resources/application.properties"));

        configs.forEach(conf -> Assertions.assertTrue(propertiesFile.contains(conf)));
    }

    @Test
    @Tag("failsOnJDK16")
    public void testCreateGradleDefaults() throws Exception {
        Path project = workspace.resolve("code-with-quarkus");

        execute("create", "--gradle");

        Assertions.assertEquals(CommandLine.ExitCode.OK, exitCode);
        Assertions.assertTrue(screen.contains("Project code-with-quarkus created"));

        Assertions.assertTrue(project.resolve("build.gradle").toFile().exists());
        String pom = readString(project.resolve("build.gradle"));
        Assertions.assertTrue(pom.contains("group 'org.acme'"));
        Assertions.assertTrue(pom.contains("version '1.0.0-SNAPSHOT'"));
        Assertions.assertTrue(project.resolve("settings.gradle").toFile().exists());
        String settings = readString(project.resolve("settings.gradle"));
        Assertions.assertTrue(settings.contains("code-with-quarkus"));

    }

    @Test
    @Tag("failsOnJDK16")
    public void testGradleBuild() throws Exception {

        execute("create", "--gradle", "resteasy");

        Path project = workspace.resolve("code-with-quarkus");
        System.setProperty("user.dir", project.toFile().getAbsolutePath());
        Assertions.assertEquals(CommandLine.ExitCode.OK, exitCode);

        execute("build");
        Assertions.assertEquals(CommandLine.ExitCode.OK, exitCode);

        execute("clean");
        Assertions.assertEquals(CommandLine.ExitCode.OK, exitCode);

    }

    @Test
    public void testMavenBuild() throws Exception {

        execute("create", "resteasy");

        Path project = workspace.resolve("code-with-quarkus");
        System.setProperty("user.dir", project.toFile().getAbsolutePath());
        Assertions.assertEquals(CommandLine.ExitCode.OK, exitCode);

        execute("build");
        Assertions.assertEquals(CommandLine.ExitCode.OK, exitCode);

        execute("clean");
        Assertions.assertEquals(CommandLine.ExitCode.OK, exitCode);

    }

    @Test
    public void testCreateJBangRestEasy() throws Exception {

        execute("create-jbang", "--output-folder=my-jbang-project", "resteasy-jsonb");

        Path project = workspace.resolve("my-jbang-project");
        System.setProperty("user.dir", project.toFile().getAbsolutePath());
        Assertions.assertEquals(CommandLine.ExitCode.OK, exitCode);
    }

    @Test
    public void testCreateJBangPicocli() throws Exception {

        execute("create-jbang", "--output-folder=my-jbang-project", "quarkus-picocli");

        Path project = workspace.resolve("my-jbang-project");
        System.setProperty("user.dir", project.toFile().getAbsolutePath());
        Assertions.assertEquals(CommandLine.ExitCode.OK, exitCode);
    }

    private void deleteDir(Path path) throws Exception {
        if (!path.toFile().exists())
            return;
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    private void execute(String... args) throws Exception {
        System.out.print("$ quarkus");
        args = ExecuteUtil.prependArray("-e", args);
        args = ExecuteUtil.prependArray("--verbose", args);
        args = ExecuteUtil.prependArray("--manual-output", args);
        for (String arg : args)
            System.out.print(" " + arg);
        System.out.println();
        PrintStream stdout = System.out;
        PrintStream stderr = System.err;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        System.setOut(ps);
        System.setErr(ps);

        QuarkusCli cli = new QuarkusCli();
        try {
            exitCode = cli.run(args);
            ps.flush();
        } finally {
            System.setOut(stdout);
            System.setErr(stderr);
        }
        screen = baos.toString();
        System.out.println(screen);
    }

    private String readString(Path path) throws Exception {
        return new String(Files.readAllBytes(path));
    }
}
