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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import org.eclipse.microprofile.graphql.tck.dynamic.ExecutionDynamicTest;
import org.eclipse.microprofile.graphql.tck.dynamic.SchemaDynamicValidityTest;
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
    private static final String SCHEMA_TEST = SchemaDynamicValidityTest.class.getName();

    private static final String TEST_SPECIFICATION = "testSpecification";

    private static final String OVERRIDES = "src/main/resources/overrides";

    private static final String PIPE = "|";
    private static final String FILE_TYPE = ".csv";
    private static final String DELIMITER = "\\" + PIPE;
    private static final String COMMENT = "#";
    private static final String OR = "'OR'";

    private Map<String, org.eclipse.microprofile.graphql.tck.dynamic.execution.TestData> executionTestDataMap = new HashMap<>();
    private Map<Integer, org.eclipse.microprofile.graphql.tck.dynamic.schema.TestData> schemaTestDataMap = new HashMap<>();

    public TestInterceptor() {
        loadExecutionOverride();
        loadSchemaOverride();
    }

    private void loadSchemaOverride() {
        Path folderPath = Paths.get(OVERRIDES);
        if (!Files.isDirectory(folderPath)) {
            return;
        }
        // Get all csv files
        try (DirectoryStream<Path> overrides = Files.newDirectoryStream(folderPath)) {
            List<Path> overrideFiles = toListOfPathsForSchema(overrides);
            List<org.eclipse.microprofile.graphql.tck.dynamic.schema.TestData> testDataList = toListOfTestDataForSchema(
                    overrideFiles);

            for (org.eclipse.microprofile.graphql.tck.dynamic.schema.TestData td : testDataList) {
                schemaTestDataMap.put(generateKey(td), td);
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void loadExecutionOverride() {
        Path folderPath = Paths.get(OVERRIDES);
        if (!Files.isDirectory(folderPath)) {
            return;
        }
        try (DirectoryStream<Path> overrides = Files.newDirectoryStream(folderPath)) {
            Set<Path> overrideFolders = toListOfPathsForExecution(overrides);
            executionTestDataMap.putAll(toMapOfTestDataForExecution(overrideFolders));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private Set<Path> toListOfPathsForExecution(DirectoryStream<Path> directoryStream) {
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

    private static List<Path> toListOfPathsForSchema(DirectoryStream<Path> directoryStream) {
        List<Path> files = new ArrayList<>();
        for (Path p : directoryStream) {
            if (!Files.isDirectory(p) && p.getFileName().toString().endsWith(FILE_TYPE)) {
                files.add(p);
            }
        }
        return files;
    }

    private Map<String, org.eclipse.microprofile.graphql.tck.dynamic.execution.TestData> toMapOfTestDataForExecution(
            Set<Path> testFolders) {
        Map<String, org.eclipse.microprofile.graphql.tck.dynamic.execution.TestData> testDataMap = new HashMap<>();
        for (Path testFolder : testFolders) {
            if (!testFolder.getFileName().toString().startsWith("META-INF")) {// Ignore META-INF
                try {
                    org.eclipse.microprofile.graphql.tck.dynamic.execution.TestData testData = toTestDataForExecution(
                            testFolder);
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

    private List<org.eclipse.microprofile.graphql.tck.dynamic.schema.TestData> toListOfTestDataForSchema(
            List<Path> testFolders) {
        List<org.eclipse.microprofile.graphql.tck.dynamic.schema.TestData> testDataList = new LinkedList<>();
        for (Path testFile : testFolders) {
            try {
                testDataList.addAll(toTestDataForSchema(testFile));
            } catch (IOException ioe) {
                LOG.logf(Logger.Level.ERROR, "Could not add test case {0} - {1}",
                        new Object[] { testFile.getFileName().toString(), ioe.getMessage() });
            }

        }
        return testDataList;
    }

    private org.eclipse.microprofile.graphql.tck.dynamic.execution.TestData toTestDataForExecution(Path folder)
            throws IOException {
        org.eclipse.microprofile.graphql.tck.dynamic.execution.TestData testData = new org.eclipse.microprofile.graphql.tck.dynamic.execution.TestData(
                folder.getFileName().toString().replace("/", ""));
        Files.walkFileTree(folder, new HashSet<>(), 1, new FileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {

                if (!Files.isDirectory(file)) {
                    String filename = file.getFileName().toString();

                    if (filename.matches("output.*\\.json")) {
                        // Special case to cater for multiple output*.json files where the
                        // test will pass on the first file content that matches.
                        // If no content matches, then the test will fail.
                        String content = getFileContent(file);
                        testData.addOutput(content);
                    } else if (filename.matches("input.*\\.graphql")) {
                        // Special case to cater for multiple input*.graphql files where the
                        // test will pass on the first file input content which is successful.
                        // If no content matches, then the test will fail.
                        String content = getFileContent(file);
                        testData.addInput(content);
                    } else {
                        switch (filename) {
                            case "httpHeader.properties": {
                                Properties properties = new Properties();
                                properties.load(Files.newInputStream(file));
                                testData.setHttpHeaders(properties);
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

    private static List<org.eclipse.microprofile.graphql.tck.dynamic.schema.TestData> toTestDataForSchema(Path testFile)
            throws IOException {
        List<org.eclipse.microprofile.graphql.tck.dynamic.schema.TestData> testDataList = new LinkedList<>();
        List<String> content = Files.readAllLines(testFile);
        String currentHeader = "";
        for (String line : content) {
            if (validLine(line)) {
                String[] parts = line.split(DELIMITER);
                if (parts.length == 4) {
                    org.eclipse.microprofile.graphql.tck.dynamic.schema.TestData testData = createTestDataForSchema(
                            currentHeader, testFile.getFileName().toString(), parts);
                    testDataList.add(testData);
                } else {
                    LOG.logf(Logger.Level.ERROR, "Could not add test case {0} - {1}",
                            new Object[] { testFile.getFileName().toString(),
                                    "Does not contain 3 parts [" + parts.length + "]" });
                }
            } else if (isHeader(line)) {
                currentHeader = line.substring(line.indexOf(COMMENT) + 1).trim();
            }
        }
        return testDataList;
    }

    private static org.eclipse.microprofile.graphql.tck.dynamic.schema.TestData createTestDataForSchema(String header,
            String filename, String[] parts) {
        org.eclipse.microprofile.graphql.tck.dynamic.schema.TestData testData = new org.eclipse.microprofile.graphql.tck.dynamic.schema.TestData();
        testData.setCount(Integer.valueOf(parts[0]));
        testData.setHeader(header);
        testData.setName(filename);
        String count = parts[0].trim();
        String snippet = parts[1].trim();
        if (snippet == null || snippet.isEmpty()) {
            snippet = null;
        }
        testData.setSnippetSearchTerm(snippet);

        String containsString = parts[2].trim();
        if (containsString.contains(OR)) {
            String[] containsStrings = containsString.split(OR);
            for (String oneOf : containsStrings) {
                testData.addContainsString(oneOf.trim());
            }
        } else {
            testData.addContainsString(containsString);
        }
        testData.setErrorMessage("(" + count + ") - " + parts[3].trim());

        return testData;
    }

    private static boolean validLine(String line) {
        return !line.isEmpty() && line.trim().contains(PIPE) && !isHeader(line);
    }

    private static boolean isHeader(String line) {
        return line.trim().startsWith(COMMENT);
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
            onExecutionTestStart(itr);
        } else if (itr.getTestClass().getName().equals(SCHEMA_TEST)) {
            onSchemaTestStart(itr);
        }
        super.onTestStart(itr);

    }

    private void onSchemaTestStart(ITestResult itr) {
        if (itr.getName().equals("testPartsOfSchema")) { // We only override the input data
            org.eclipse.microprofile.graphql.tck.dynamic.schema.TestData testData = getSchemaTestData(itr.getParameters());

            int key = generateKey(testData);
            if (!schemaTestDataMap.isEmpty() && schemaTestDataMap.containsKey(key)) {
                LOG.info("\n\t =================================================="
                        + "\n\t Schema: Testing Override [" + testData.getCount() + "] " + testData.getHeader() + " in "
                        + testData.getName()
                        + "\n\t =================================================="
                        + "\n");
                org.eclipse.microprofile.graphql.tck.dynamic.schema.TestData overrideTestData = schemaTestDataMap.get(key);
                testData.setContainsAnyOfString(overrideTestData.getContainsAnyOfString());
                testData.setErrorMessage(overrideTestData.getErrorMessage());
                testData.setSnippetSearchTerm(overrideTestData.getSnippetSearchTerm());
            }
        }
    }

    private void onExecutionTestStart(ITestResult itr) {
        org.eclipse.microprofile.graphql.tck.dynamic.execution.TestData testData = getExecutionTestData(itr.getParameters());

        LOG.info("\n\t =================================================="
                + "\n\t Execution: Testing [" + testData.getName() + "]"
                + "\n\t =================================================="
                + "\n");

        if (itr.getName().startsWith(TEST_SPECIFICATION) && !executionTestDataMap.isEmpty()
                && executionTestDataMap.containsKey(testData.getName())) {

            org.eclipse.microprofile.graphql.tck.dynamic.execution.TestData override = executionTestDataMap
                    .get(testData.getName());

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

    private org.eclipse.microprofile.graphql.tck.dynamic.execution.TestData getExecutionTestData(Object[] parameters) {
        for (Object param : parameters) {
            if (org.eclipse.microprofile.graphql.tck.dynamic.execution.TestData.class.isInstance(param)) {
                return org.eclipse.microprofile.graphql.tck.dynamic.execution.TestData.class.cast(param);
            }
        }
        return null;
    }

    private org.eclipse.microprofile.graphql.tck.dynamic.schema.TestData getSchemaTestData(Object[] parameters) {
        for (Object param : parameters) {
            if (org.eclipse.microprofile.graphql.tck.dynamic.schema.TestData.class.isInstance(param)) {
                return org.eclipse.microprofile.graphql.tck.dynamic.schema.TestData.class.cast(param);
            }
        }
        throw new RuntimeException("Could not find TestData for SchemaTest");
    }

    private int generateKey(org.eclipse.microprofile.graphql.tck.dynamic.schema.TestData data) {
        String concat = data.getName() + data.getHeader() + data.getCount();
        return concat.hashCode();
    }

}
