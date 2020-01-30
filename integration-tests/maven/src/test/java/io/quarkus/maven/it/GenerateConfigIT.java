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
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.InvokerLogger;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.invoker.PrintStreamLogger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@DisableForNative
class GenerateConfigIT extends QuarkusPlatformAwareMojoTestBase {

    private static final String PROJECT_SOURCE_DIR = "projects/classic";
    private File testDir;
    private Invoker invoker;

    @Test
    void testAddExtensionWithASingleExtension() throws MavenInvocationException, IOException {
        testDir = initProject(PROJECT_SOURCE_DIR, "projects/testGenerateConfig");
        invoker = initInvoker(testDir);
        generateConfig("test.properties");

        String file = loadFile("test.properties");
        Assertions.assertTrue(file.contains("#quarkus.log.level"));
        Assertions.assertTrue(file.contains("The default log level"));
        Assertions.assertTrue(file.contains("#quarkus.thread-pool.growth-resistance=0"));
        Assertions.assertTrue(file.contains("The executor growth resistance"));

        generateConfig("application.properties");
        //the existing file should not add properties that already exist
        file = loadFile("application.properties");
        Assertions.assertTrue(file.contains("quarkus.log.level=INFO"));
        Assertions.assertFalse(file.contains("The default log level"));
        Assertions.assertTrue(file.contains("#quarkus.thread-pool.growth-resistance=0"));
        Assertions.assertTrue(file.contains("The executor growth resistance"));
    }

    private String loadFile(String file) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(new File(testDir, "src/main/resources/" + file)), "UTF-8"))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    private void generateConfig(String filename)
            throws MavenInvocationException, FileNotFoundException, UnsupportedEncodingException {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setBatchMode(true);
        request.setGoals(Collections
                .singletonList(getPluginGroupId() + ":" + getPluginArtifactId() + ":"
                        + getPluginVersion() + ":generate-config"));
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
