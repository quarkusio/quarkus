package io.quarkus.maven.it;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvokerLogger;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.invoker.PrintStreamLogger;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import io.quarkus.maven.CreateProjectMojo;
import io.quarkus.maven.utilities.MojoUtils;

class GenerateConfigIT extends MojoTestBase {

    private static final String PROJECT_SOURCE_DIR = "projects/classic";
    private File testDir;
    private DefaultInvoker invoker;

    @Test
    void testAddExtensionWithASingleExtension() throws MavenInvocationException, IOException {
        testDir = initProject(PROJECT_SOURCE_DIR, "projects/testGenerateConfig");
        init(testDir);
        generateConfig("test.properties");

        String file = loadFile("test.properties");
        Assert.assertTrue(file.contains("#quarkus.log.file.enable"));
        Assert.assertTrue(file.contains("If file logging should be enabled"));
        Assert.assertTrue(file.contains("#quarkus.thread-pool.growth-resistance=0"));
        Assert.assertTrue(file.contains("The executor growth resistance"));

        generateConfig("application.properties");
        //the existing file should not add properties that already exist
        file = loadFile("application.properties");
        Assert.assertTrue(file.contains("quarkus.log.file.enable=false"));
        Assert.assertFalse(file.contains("If file logging should be enabled"));
        Assert.assertTrue(file.contains("#quarkus.thread-pool.growth-resistance=0"));
        Assert.assertTrue(file.contains("The executor growth resistance"));
    }

    private String loadFile(String file) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(new File(testDir, "src/main/resources/" + file)), "UTF-8"))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    private void init(File root) {
        invoker = new DefaultInvoker();
        invoker.setWorkingDirectory(root);
        String repo = System.getProperty("maven.repo");
        if (repo == null) {
            repo = new File(System.getProperty("user.home"), ".m2/repository").getAbsolutePath();
        }
        invoker.setLocalRepositoryDirectory(new File(repo));
        installPluginToLocalRepository(invoker.getLocalRepositoryDirectory());
    }

    private void generateConfig(String filename)
            throws MavenInvocationException, FileNotFoundException, UnsupportedEncodingException {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setBatchMode(true);
        request.setDebug(true);
        request.setGoals(Collections
                .singletonList(CreateProjectMojo.PLUGIN_KEY + ":" + MojoUtils.getPluginVersion() + ":generate-config"));
        Properties properties = new Properties();
        properties.setProperty("file", filename);
        request.setProperties(properties);
        getEnv().forEach(request::addShellEnvironment);
        File log = new File(testDir, "build-generate-config-" + testDir.getName() + ".log");
        PrintStreamLogger logger = new PrintStreamLogger(new PrintStream(new FileOutputStream(log), false, "UTF-8"),
                InvokerLogger.DEBUG);
        invoker.setLogger(logger);
        invoker.execute(request);
    }
}
