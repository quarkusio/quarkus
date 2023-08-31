package io.quarkus.deployment.dev.testing;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.runtime.util.ClassPathUtils;

/**
 * Maps between builder test and application class directories.
 */
public final class PathTestHelper {
    private static final String TARGET = "target";
    private static final Map<String, String> TEST_TO_MAIN_DIR_FRAGMENTS = new HashMap<>();

    static {
        //region Eclipse
        TEST_TO_MAIN_DIR_FRAGMENTS.put(
                "bin" + File.separator + "test",
                "bin" + File.separator + "main");
        //endregion

        //region Idea
        TEST_TO_MAIN_DIR_FRAGMENTS.put(
                "out" + File.separator + "test",
                "out" + File.separator + "production");
        //endregion

        // region Gradle
        // region Java
        TEST_TO_MAIN_DIR_FRAGMENTS.put(
                "classes" + File.separator + "java" + File.separator + "native-test",
                "classes" + File.separator + "java" + File.separator + "main");
        TEST_TO_MAIN_DIR_FRAGMENTS.put(
                "classes" + File.separator + "java" + File.separator + "test",
                "classes" + File.separator + "java" + File.separator + "main");
        TEST_TO_MAIN_DIR_FRAGMENTS.put(
                "classes" + File.separator + "java" + File.separator + "integration-test",
                "classes" + File.separator + "java" + File.separator + "main");
        TEST_TO_MAIN_DIR_FRAGMENTS.put(
                "classes" + File.separator + "java" + File.separator + "integrationTest",
                "classes" + File.separator + "java" + File.separator + "main");
        TEST_TO_MAIN_DIR_FRAGMENTS.put(
                "classes" + File.separator + "java" + File.separator + "native-integrationTest",
                "classes" + File.separator + "java" + File.separator + "main");
        TEST_TO_MAIN_DIR_FRAGMENTS.put(
                "classes" + File.separator + "java" + File.separator + "native-integration-test",
                "classes" + File.separator + "java" + File.separator + "main");
        TEST_TO_MAIN_DIR_FRAGMENTS.put( //synthetic tmp dirs when there are multiple outputs
                "quarkus-app-classes-test",
                "quarkus-app-classes");
        //endregion
        //region Kotlin
        TEST_TO_MAIN_DIR_FRAGMENTS.put(
                "classes" + File.separator + "kotlin" + File.separator + "native-test",
                "classes" + File.separator + "kotlin" + File.separator + "main");
        TEST_TO_MAIN_DIR_FRAGMENTS.put(
                "classes" + File.separator + "kotlin" + File.separator + "test",
                "classes" + File.separator + "kotlin" + File.separator + "main");
        TEST_TO_MAIN_DIR_FRAGMENTS.put(
                "classes" + File.separator + "kotlin" + File.separator + "integration-test",
                "classes" + File.separator + "kotlin" + File.separator + "main");
        TEST_TO_MAIN_DIR_FRAGMENTS.put(
                "classes" + File.separator + "kotlin" + File.separator + "integrationTest",
                "classes" + File.separator + "kotlin" + File.separator + "main");
        TEST_TO_MAIN_DIR_FRAGMENTS.put(
                "classes" + File.separator + "kotlin" + File.separator + "native-integrationTest",
                "classes" + File.separator + "kotlin" + File.separator + "main");
        TEST_TO_MAIN_DIR_FRAGMENTS.put(
                "classes" + File.separator + "kotlin" + File.separator + "native-integration-test",
                "classes" + File.separator + "kotlin" + File.separator + "main");
        //endregion
        //region Scala
        TEST_TO_MAIN_DIR_FRAGMENTS.put(
                "classes" + File.separator + "scala" + File.separator + "native-test",
                "classes" + File.separator + "scala" + File.separator + "main");
        TEST_TO_MAIN_DIR_FRAGMENTS.put(
                "classes" + File.separator + "scala" + File.separator + "test",
                "classes" + File.separator + "scala" + File.separator + "main");
        TEST_TO_MAIN_DIR_FRAGMENTS.put(
                "classes" + File.separator + "scala" + File.separator + "integration-test",
                "classes" + File.separator + "scala" + File.separator + "main");
        TEST_TO_MAIN_DIR_FRAGMENTS.put(
                "classes" + File.separator + "scala" + File.separator + "integrationTest",
                "classes" + File.separator + "scala" + File.separator + "main");
        TEST_TO_MAIN_DIR_FRAGMENTS.put(
                "classes" + File.separator + "scala" + File.separator + "native-integrationTest",
                "classes" + File.separator + "scala" + File.separator + "main");
        TEST_TO_MAIN_DIR_FRAGMENTS.put(
                "classes" + File.separator + "scala" + File.separator + "native-integration-test",
                "classes" + File.separator + "scala" + File.separator + "main");
        //endregion
        //endregion

        //region Maven
        TEST_TO_MAIN_DIR_FRAGMENTS.put(
                File.separator + "test-classes",
                File.separator + "classes");
        //endregion

        String mappings = System.getenv(BootstrapConstants.TEST_TO_MAIN_MAPPINGS);
        if (mappings != null) {
            Stream.of(mappings.split(","))
                    .filter(s -> !s.isEmpty())
                    .forEach(s -> {
                        String[] entry = s.split(":");
                        if (entry.length == 2) {
                            TEST_TO_MAIN_DIR_FRAGMENTS.put(entry[0], entry[1]);
                        } else {
                            throw new IllegalStateException("Unable to parse additional test-to-main mapping: " + s);
                        }
                    });
        }
    }

    private PathTestHelper() {
    }

    /**
     * We normally start with a test class and work out the runtime classpath
     * from that;
     * here we need to go in the other direction. There's probably existing logic we
     * can re-use, because this
     * is seriously brittle
     * This method finds directories not in BootstrapConstants.OUTPUT_SOURCES_DIR (in the case of Gradle additional sources)
     */
    // TODO can be deleted now that we have changed the quarkus plugin?
    public static Path getTestClassesLocationWithNoContext() {

        // TODO can we be more elegant about this? What about multi-module?
        // TODO should we cross-reference against the classpath?
        Path projectRoot = Paths.get("")
                .normalize()
                .toAbsolutePath();

        Path applicationRoot = null;

        // TODO this cannot be the right pattern because we don't do it anywhere else, but
        String[] ses = System.getProperty("java.class.path")
                .split(File.pathSeparator);
        for (String s : ses) {
            Path path = Paths.get(s);
            if (path.normalize()
                    .toAbsolutePath()
                    .startsWith(projectRoot)) {
                System.out.println("CANDIDATE CLASSPATH " + s);
                // TODO ugly; set it if we didn't set it to something on the classpath
                // TODO fragile, we miss other modules in multi-module
                //  things like us to take the first element on the classpath that fits
                // TODO we take the first classpath that matches, which is rather arbitrary and brittle

                // The application root needs to be a directory that holds test classes `(or TODO maybe application classes)
                if (applicationRoot == null) {
                    applicationRoot = path;
                    System.out.println("made app root" + applicationRoot);
                    // TODO we do not break so we can continue logging
                }
            }
        }

        if (applicationRoot == null) {
            throw new RuntimeException("Could not find any elements of the classpath inside the project root " + projectRoot);
        }

        return applicationRoot;
    }

    /**
     * Resolves the directory or the JAR file containing the test class.
     *
     * @param testClass the test class
     * @return directory or JAR containing the test class
     */
    public static Path getTestClassesLocation(Class<?> testClass) {
        String classFileName = testClass.getName().replace('.', File.separatorChar) + ".class";
        URL resource = testClass.getClassLoader().getResource(testClass.getName().replace('.', '/') + ".class");

        if (resource.getProtocol().equals("jar")) {
            try {
                resource = URI.create(resource.getFile().substring(0, resource.getFile().indexOf('!'))).toURL();
                return toPath(resource);
            } catch (MalformedURLException e) {
                throw new RuntimeException("Failed to resolve the location of the JAR containing " + testClass, e);
            }
        }
        Path path = toPath(resource);
        path = path.getRoot().resolve(path.subpath(0, path.getNameCount() - Path.of(classFileName).getNameCount()));

        if (!isInTestDir(resource) && !path.getParent().getFileName().toString().equals(TARGET)) {
            final StringBuilder msg = new StringBuilder();
            msg.append("The test class ").append(testClass.getName()).append(" is not located in any of the directories ");
            var i = TEST_TO_MAIN_DIR_FRAGMENTS.keySet().iterator();
            msg.append(i.next());
            while (i.hasNext()) {
                msg.append(", ").append(i.next());
            }
            throw new RuntimeException(msg.toString());
        }
        return path;
    }

    /**
     * Resolves the directory or the JAR file containing the application being tested by the test class.
     *
     * @param testClass the test class
     * @return directory or JAR containing the application being tested by the test class
     */
    public static Path getAppClassLocation(Class<?> testClass) {
        return getAppClassLocationForTestLocation(getTestClassesLocation(testClass).toString());
    }

    public static Path getAppClassLocationForRootLocation(String rootLocation) {
        if (rootLocation.endsWith(".jar")) {
            if (rootLocation.endsWith("-tests.jar")) {
                return Paths.get(new StringBuilder()
                        .append(rootLocation, 0, rootLocation.length() - "-tests.jar".length())
                        .append(".jar")
                        .toString());
            }
            return Path.of(rootLocation);
        }
        Optional<Path> mainClassesDir = TEST_TO_MAIN_DIR_FRAGMENTS.entrySet()
                .stream()
                .map(e -> {
                    return Path.of(
                            rootLocation + File.separator + e.getValue());
                })
                .filter(path -> Files.exists(path))
                .findFirst();
        if (mainClassesDir.isPresent()) {
            System.out.println("WAHOO GOT A MAIN PATH" + mainClassesDir.get());
            return mainClassesDir.get();
        }
        throw new IllegalStateException("Unable to find any application content in " + rootLocation);
    }

    /**
     * Resolves the directory or the JAR file containing the application being tested by a test from the given location.
     *
     * @param testClassLocation the test class location
     * @return directory or JAR containing the application being tested by a test from the given location
     */
    public static Path getAppClassLocationForTestLocation(String testClassLocation) {
        if (testClassLocation.endsWith(".jar")) {
            if (testClassLocation.endsWith("-tests.jar")) {
                return Paths.get(new StringBuilder()
                        .append(testClassLocation, 0, testClassLocation.length() - "-tests.jar".length())
                        .append(".jar")
                        .toString());
            }
            return Path.of(testClassLocation);
        }
        Optional<Path> mainClassesDir = TEST_TO_MAIN_DIR_FRAGMENTS.entrySet().stream()
                .filter(e -> testClassLocation.contains(e.getKey()))
                .map(e -> {
                    // we should replace only the last occurrence of the fragment
                    final int i = testClassLocation.lastIndexOf(e.getKey());
                    final StringBuilder buf = new StringBuilder(testClassLocation.length());
                    buf.append(testClassLocation.substring(0, i)).append(e.getValue());
                    if (i + e.getKey().length() + 1 < testClassLocation.length()) {
                        buf.append(testClassLocation.substring(i + e.getKey().length()));
                    }
                    return Path.of(buf.toString());
                })
                .findFirst();
        Path p = null;
        if (mainClassesDir.isPresent()) {
            p = mainClassesDir.get();
            if (Files.exists(p)) {
                return p;
            }
        }
        // could be a custom test classes dir under the 'target' dir with the main
        // classes still under 'target/classes'
        p = Path.of(testClassLocation).getParent();
        if (p != null && p.getFileName().toString().equals(TARGET)) {
            p = p.resolve("classes");
            if (Files.exists(p)) {
                return p;
            }
        }
        if (mainClassesDir.isPresent()) {
            // if it's mapped but doesn't exist, it's still ok to return it
            return mainClassesDir.get();
        }
        throw new IllegalStateException("Unable to translate path for " + testClassLocation);
    }

    /**
     * Returns the resources directory that compliments the classes directory.
     * This is relevant in for Gradle where classes and resources have different output locations.
     * The method will return null if classesDir is not a directory.
     *
     * @param classesDir classes directory
     * @param name 'test' for test resources or 'main' for the main resources
     * @return resources directory if found or null otherwise
     */
    public static Path getResourcesForClassesDirOrNull(Path classesDir, String name) {
        if (!Files.isDirectory(classesDir)) {
            return null;
        }
        Path p = classesDir.getParent();
        if (p == null) {
            return null;
        }
        p = p.getParent();
        if (p == null) {
            return null;
        }
        p = p.getParent();
        if (p == null) {
            return null;
        }
        p = p.resolve("resources").resolve(name);
        if (Files.exists(p)) {
            return p;
        }
        return null;
    }

    public static boolean isTestClass(String className, ClassLoader classLoader, Path testLocation) {
        URL resource = classLoader.getResource(className.replace('.', '/') + ".class");
        if (resource == null) {
            return false;
        }
        if (Files.isDirectory(testLocation)) {
            return resource.getProtocol().startsWith("file") && toPath(resource).startsWith(testLocation);
        }
        if (!resource.getProtocol().equals("jar")) {
            return false;
        }
        String path = resource.getPath();
        if (!path.startsWith("file:")) {
            return false;
        }
        path = path.substring(5, path.lastIndexOf('!'));

        return testLocation.equals(Path.of(path));
    }

    private static boolean isInTestDir(URL resource) {
        String path = toPath(resource).toString();
        return TEST_TO_MAIN_DIR_FRAGMENTS.keySet().stream()
                .anyMatch(path::contains);
    }

    private static Path toPath(URL resource) {
        return ClassPathUtils.toLocalPath(resource);
    }

    /**
     * Returns the build directory of the project given its base dir and the test classes dir.
     *
     * @param projectRoot project dir
     * @param testClassLocation test dir
     *
     * @return project build dir
     */
    public static Path getProjectBuildDir(Path projectRoot, Path testClassLocation) {
        if (!testClassLocation.startsWith(projectRoot)) {
            // this typically happens in the platform testsuite where test classes are loaded from jars
            return projectRoot.resolve("target");
        }
        return projectRoot.resolve(projectRoot.relativize(testClassLocation).getName(0));
    }
}
