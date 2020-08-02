package io.quarkus.tck.graphql;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.eclipse.microprofile.graphql.tck.dynamic.ExecutionDynamicTest;
import org.eclipse.microprofile.graphql.tck.dynamic.execution.TestData;
import org.jboss.logging.Logger;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;

/**
 * This allows us to override the input and output of the spec's TCK.
 * Sometimes this is needed when the spec should be updated but can not yet
 * 
 */
public class TestInterceptor extends TestListenerAdapter {
    private static final Logger LOG = Logger.getLogger(TestInterceptor.class.getName());

    private static final String EXECUTION_TEST = ExecutionDynamicTest.class.getName();
    private static final String TEST_SPECIFICATION = "testSpecification";

    private static final String OVERRIDES = "src/main/resources/overrides";

    private Map<String, TestData> testDataMap = new HashMap<>();

    public TestInterceptor() {
        try {
            Path folderPath = Paths.get(OVERRIDES);
            DirectoryStream<Path> overrides = Files.newDirectoryStream(folderPath);
            Set<Path> overrideFolders = toListOfPaths(overrides);
            testDataMap.putAll(toMapOfTestData(overrideFolders));
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    private Set<Path> toListOfPaths(DirectoryStream<Path> directoryStream) {
        Set<Path> directories = new HashSet<>();
        for (Path p : directoryStream) {
            try (Stream<Path> paths = Files.walk(p)) {
                Set<Path> tree = paths.filter(Files::isDirectory)
                        .collect(Collectors.toSet());
                directories.addAll(tree);
            } catch (IOException ex) {
                LOG.errorf("Ignoring directory [{0}] - {1}", new Object[] { p.getFileName().toString(), ex.getMessage() });
            }
        }
        return directories;
    }

    private Map<String, TestData> toMapOfTestData(Set<Path> testFolders) {
        Map<String, TestData> testDataMap = new HashMap<>();
        for (Path testFolder : testFolders) {
            if (!testFolder.getFileName().toString().startsWith("META-INF")) {// Ignore META-INF
                try {
                    TestData testData = toTestData(testFolder);
                    if (!testData.shouldIgnore()) {
                        testDataMap.put(testData.getName(), testData);
                    }
                } catch (IOException ioe) {
                    LOG.errorf("Could not add test case {0} - {1}",
                            new Object[] { testFolder.getFileName().toString(), ioe.getMessage() });
                }
            }
        }
        return testDataMap;
    }

    private TestData toTestData(Path folder) throws IOException {
        TestData testData = new TestData(folder.getFileName().toString().replace("/", ""));
        Files.walkFileTree(folder, new HashSet<>(), 1, new FileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {

                if (!Files.isDirectory(file)) {
                    String filename = file.getFileName().toString();

                    switch (filename) {
                        case "input.graphql": {
                            String content = getFileContent(file);
                            testData.setInput(content);
                            break;
                        }
                        case "httpHeader.properties": {
                            Properties properties = new Properties();
                            properties.load(Files.newInputStream(file));
                            testData.setHttpHeaders(properties);
                            break;
                        }
                        case "output.json": {
                            String content = getFileContent(file);
                            testData.setOutput(content);
                            break;
                        }
                        case "variables.json": {
                            String content = getFileContent(file);
                            testData.setVariables(toJsonObject(content));
                            break;
                        }
                        case "test.properties": {
                            Properties properties = new Properties();
                            properties.load(Files.newInputStream(file));
                            testData.setProperties(properties);
                            break;
                        }
                        case "cleanup.graphql": {
                            String content = getFileContent(file);
                            testData.setCleanup(content);
                            break;
                        }
                        case "prepare.graphql": {
                            String content = getFileContent(file);
                            testData.setPrepare(content);
                            break;
                        }
                        default:
                            LOG.warnf("Ignoring unknown file {0}", filename);
                            break;
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc)
                    throws IOException {
                LOG.errorf("Could not load file {0}[{1}]", new Object[] { file, exc.getMessage() });
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });

        return testData;
    }

    private JsonObject toJsonObject(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) {
            return null;
        }
        try (JsonReader jsonReader = Json.createReader(new StringReader(jsonString))) {
            return jsonReader.readObject();
        }
    }

    private String getFileContent(Path file) throws IOException {
        return new String(Files.readAllBytes(file));
    }

    @Override
    public void onTestStart(ITestResult itr) {

        if (itr.getTestClass().getName().equals(EXECUTION_TEST)) {
            TestData testData = getExecutionTestData(itr.getParameters());

            LOG.info("\n\t =================================================="
                    + "\n\t Testing [" + testData.getName() + "]"
                    + "\n\t =================================================="
                    + "\n");

            if (itr.getName().startsWith(TEST_SPECIFICATION) && !testDataMap.isEmpty()
                    && testDataMap.containsKey(testData.getName())) {

                TestData override = testDataMap.get(testData.getName());

                if (override.getCleanup() != null && !override.getCleanup().isEmpty()) {
                    testData.setCleanup(override.getCleanup());
                    LOG.warn("\n\t NOTE: Overriding Cleanup");
                }
                if (override.getHttpHeaders() != null && !override.getHttpHeaders().isEmpty()) {
                    testData.setHttpHeaders(override.getHttpHeaders());
                    LOG.warn("\n\t NOTE: Overriding HTTP Headers");
                }
                if (override.getInput() != null && !override.getInput().isEmpty()) {
                    testData.setInput(override.getInput());
                    LOG.warn("\n\t NOTE: Overriding Input");
                }
                if (override.getOutput() != null && !override.getOutput().isEmpty()) {
                    testData.setOutput(override.getOutput());
                    LOG.warn("\n\t NOTE: Overriding Output");
                }
                if (override.getPrepare() != null && !override.getPrepare().isEmpty()) {
                    testData.setPrepare(override.getPrepare());
                    LOG.warn("\n\t NOTE: Overriding Prepare");
                }
                if (override.getProperties() != null && !override.getProperties().isEmpty()) {
                    testData.setProperties(override.getProperties());
                    LOG.warn("\n\t NOTE: Overriding Properties");
                }
                if (override.getVariables() != null && !override.getVariables().isEmpty()) {
                    testData.setVariables(override.getVariables());
                    LOG.warn("\n\t NOTE: Overriding Variables");
                }
            }

        }
        super.onTestStart(itr);

    }

    private TestData getExecutionTestData(Object[] parameters) {
        for (Object param : parameters) {
            if (TestData.class.isInstance(param)) {
                return TestData.class.cast(param);
            }
        }
        return null;
    }

}
