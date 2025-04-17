package io.quarkus.test.common;

import static io.quarkus.commons.classloading.ClassLoaderHelper.fromClassNameToResourceName;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.classloading.ClassPathElement;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.workspace.ArtifactSources;
import io.quarkus.bootstrap.workspace.SourceDir;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.commons.classloading.ClassLoaderHelper;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.paths.PathTree;
import io.quarkus.paths.PathVisit;
import io.quarkus.runtime.util.ClassPathUtils;

/**
 * Maps between builder test and application class directories.
 */
public final class PathTestHelper {
    private static final String TARGET = "target";
    private static final Map<String, String> TEST_TO_MAIN_DIR_FRAGMENTS = new HashMap<>();
    private static final List<String> TEST_DIRS;

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
        TEST_DIRS = List.of(TEST_TO_MAIN_DIR_FRAGMENTS.keySet().toArray(new String[0]));
    }

    private PathTestHelper() {
    }

    /**
     * Resolves the directory or the JAR file containing the test class.
     *
     * @param testClass the test class
     * @return directory or JAR containing the test class
     */
    public static Path getTestClassesLocation(Class<?> testClass) {
        final String classFileName = fromClassNameToResourceName(testClass.getName());
        URL resource = testClass.getClassLoader().getResource(classFileName);
        if (resource == null) {
            throw new IllegalStateException(
                    "Could not find resource " + classFileName + " using class loader " + testClass.getClassLoader());
        }
        if (resource.getProtocol().equals("jar")) {
            try {
                resource = URI.create(resource.getFile().substring(0, resource.getFile().indexOf('!'))).toURL();
                return toPath(resource);
            } catch (MalformedURLException e) {
                throw new RuntimeException("Failed to resolve the location of the JAR containing " + classFileName, e);
            }
        }
        if (resource.getProtocol().equals("quarkus")) {
            // This is a bytecode enhanced class, the original class is either in the application module or a dependency
            final ApplicationModel appModel = ((QuarkusClassLoader) testClass.getClassLoader()).getCuratedApplication()
                    .getApplicationModel();

            Path testLocation = getTestClassesDirOrNull(classFileName, appModel.getApplicationModule());
            if (testLocation != null) {
                return testLocation;
            }

            // JARs containing tests will most of the time be direct test scoped dependencies (e.g. Quarkus platform testsuite).
            // Look among the direct dependencies first to optimize for the majority of cases.
            // Depending on the amount of dependencies and their ordering, could be ~15-20 times faster.
            testLocation = getTestClassLocationFromDepsOrNull(classFileName, appModel,
                    DependencyFlags.DIRECT | DependencyFlags.RUNTIME_CP);
            if (testLocation != null) {
                return testLocation;
            }
            // Look among all the runtime dependencies. We need a more efficient way to handle this case.
            // I don't think we have a test for this case.
            testLocation = getTestClassLocationFromDepsOrNull(classFileName, appModel, DependencyFlags.RUNTIME_CP);
            if (testLocation != null) {
                return testLocation;
            }
            throw new RuntimeException("Failed to locate " + classFileName + " among the application dependencies");
        }
        Path path = toPath(resource);
        path = path.getRoot().resolve(path.subpath(0, path.getNameCount() - Path.of(classFileName).getNameCount()));

        if (!isInTestDir(path) && !path.getParent().getFileName().toString().equals(TARGET)) {
            final StringBuilder msg = new StringBuilder();
            msg.append("The test class ").append(testClass.getName()).append(" is not located in any of the directories ");
            var i = TEST_DIRS.iterator();
            msg.append(i.next());
            while (i.hasNext()) {
                msg.append(", ").append(i.next());
            }
            throw new RuntimeException(msg.toString());
        }
        return path;
    }

    /**
     * Looks for a resource among the dependencies with specific flags. The method will return the first dependency
     * providing the resource of null, if none of the dependencies provide the resource.
     *
     * @param classFileName classpath resource name
     * @param appModel application model
     * @param depFlags dependency flags
     * @return the first dependency containing the resource or null, if none of the matching dependencies provide the resource
     */
    private static Path getTestClassLocationFromDepsOrNull(String classFileName, ApplicationModel appModel, int depFlags) {
        for (var d : appModel.getDependencies(depFlags)) {
            final Path root = d.getContentTree().apply(classFileName, PathTestHelper::getRootOrNull);
            if (root != null) {
                return root;
            }
        }
        return null;
    }

    public static Path getTestClassesLocation(Class<?> requiredTestClass, CuratedApplication curatedApplication) {
        final Path testClassesDir = getTestClassesDirOrNull(
                ClassLoaderHelper.fromClassNameToResourceName(requiredTestClass.getName()),
                curatedApplication.getApplicationModel().getApplicationModule());
        return testClassesDir != null ? testClassesDir : getTestClassesLocation(requiredTestClass);

    }

    /**
     * Looks for a resource in the output directories of a workspace module.
     * If a directory containing the resource could not be found, the method will return null.
     *
     * @param testClassFileName classpath resource
     * @param module workspace module
     * @return output directory containing the resource or null, in case the resource could not be found
     */
    private static Path getTestClassesDirOrNull(String testClassFileName, WorkspaceModule module) {
        ArtifactSources testSources = module.getTestSources();
        if (testSources != null) {
            PathTree paths = testSources.getOutputTree();
            var testClassesDir = paths.apply(testClassFileName, PathTestHelper::getRootOrNull);
            if (testClassesDir != null) {
                return testClassesDir;
            }
        }
        // If there were no test sources, this may be an application with multiple source sets; we need to search them all
        for (String classifier : module.getSourceClassifiers()) {
            final ArtifactSources sources = module.getSources(classifier);
            if (sources.isOutputAvailable()) {
                PathTree paths = sources.getOutputTree();
                var testClassesDir = paths.apply(testClassFileName, PathTestHelper::getRootOrNull);
                if (testClassesDir != null) {
                    return testClassesDir;
                }
            }
        }
        // If we got to this point, fall back to the filesystem search
        // This happens for maven source set scenarios
        // TODO getSourceClassifiers() should return the source sets in the maven case, but currently does not - see BuildIT.testCustomTestSourceSets test
        return null;
    }

    private static Path getRootOrNull(PathVisit visit) {
        if (visit == null) {
            // this path does not exist in this path tree
            return null;
        }
        return visit.getRoot();
    }

    public static void validateTestDir(Class<?> requiredTestClass, Path testClassesDir, WorkspaceModule module) {
        if (testClassesDir == null) {
            final StringBuilder sb = new StringBuilder();
            sb.append("Failed to locate ").append(requiredTestClass.getName()).append(" in ");
            for (String classifier : module.getSourceClassifiers()) {
                final ArtifactSources sources = module.getSources(classifier);
                if (sources.isOutputAvailable()) {
                    for (SourceDir d : sources.getSourceDirs()) {
                        if (Files.exists(d.getOutputDir())) {
                            sb.append(System.lineSeparator()).append(d.getOutputDir());
                        }
                    }
                }
            }
            throw new RuntimeException(sb.toString());
        }
    }

    /**
     * Resolves the directory or the JAR file containing the application being tested by a test from the given location.
     *
     * @param testClassLocationPath the test class location
     * @return directory or JAR containing the application being tested by a test from the given location
     */
    public static Path getAppClassLocationForTestLocation(Path testClassLocationPath) {
        String testClassLocation = testClassLocationPath.toString();

        if (testClassLocation.endsWith(".jar")) {
            if (testClassLocation.endsWith("-tests.jar")) {
                return Path.of(new StringBuilder()
                        .append(testClassLocation, 0, testClassLocation.length() - "-tests.jar".length())
                        .append(".jar")
                        .toString());
            }
            return testClassLocationPath;
        }
        Optional<Path> mainClassesDir = TEST_TO_MAIN_DIR_FRAGMENTS.entrySet().stream()
                .filter(e -> testClassLocation.contains(e.getKey()))
                .map(e -> {
                    // we should replace only the last occurrence of the fragment
                    final int i = testClassLocation.lastIndexOf(e.getKey());
                    final StringBuilder buf = new StringBuilder(testClassLocation.length());
                    buf.append(testClassLocation, 0, i).append(e.getValue());
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
        p = testClassLocationPath.getParent();
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
        if (classLoader instanceof QuarkusClassLoader qcl) {
            // this appears to be a more efficient and reliable way of performing this check
            // the code after this IF block has an issue on Windows
            final List<ClassPathElement> cpeList = qcl.getElementsWithResource(fromClassNameToResourceName(className),
                    false);
            if (!cpeList.isEmpty()) {
                // if it's not empty, it's pretty much always a list with a single element
                if (cpeList.size() == 1) {
                    final ClassPathElement cpe = cpeList.get(0);
                    return cpe.isRuntime() && testLocation.equals(cpe.getRoot());
                }
                for (ClassPathElement cpe : cpeList) {
                    if (cpe.isRuntime()) {
                        return cpe.getRoot().equals(testLocation);
                    }
                }
            }
            return false;
        }
        URL resource = classLoader.getResource(fromClassNameToResourceName(className));
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

    private static boolean isInTestDir(Path resource) {
        final String path = resource.toString();
        return TEST_DIRS.stream().anyMatch(path::contains);
    }

    private static Path toPath(URL resource) {
        return ClassPathUtils.toLocalPath(resource);
    }

    /**
     * Returns the build directory of the project given its base dir and the test classes dir.
     *
     * @param projectRoot project dir
     * @param testClassLocation test dir
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
