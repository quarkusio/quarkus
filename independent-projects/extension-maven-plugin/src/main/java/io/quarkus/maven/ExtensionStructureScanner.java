package io.quarkus.maven;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.platform.tools.ExtensionStructure.ClassFacts;

/**
 * Reads what {@link io.quarkus.platform.tools.ExtensionStructureChecker} needs from a class directory or a jar.
 */
final class ExtensionStructureScanner {

    private static final DotName BUILD_STEP = DotName.createSimple("io.quarkus.deployment.annotations.BuildStep");
    private static final DotName TARGET_CLASS = DotName.createSimple("com.oracle.svm.core.annotate.TargetClass");
    private static final DotName FEATURE = DotName.createSimple("org.graalvm.nativeimage.hosted.Feature");

    private ExtensionStructureScanner() {
    }

    /**
     * @param classes a class directory or a jar
     * @return the facts about each top-level class, nested classes being covered by their enclosing class
     */
    static List<ClassFacts> scan(Path classes) throws IOException {
        if (!Files.isDirectory(classes)) {
            try (FileSystem jar = FileSystems.newFileSystem(classes)) {
                return scanRoot(jar.getPath("/"));
            }
        }
        return scanRoot(classes);
    }

    /**
     * @param artifact a class directory or a jar
     * @return the {@code groupId:artifactId} of the deployment artifact, if the artifact is an extension runtime
     */
    static Optional<String> deploymentArtifactOf(Path artifact) throws IOException {
        if (!Files.isDirectory(artifact)) {
            try (FileSystem jar = FileSystems.newFileSystem(artifact)) {
                return readDeploymentArtifact(jar.getPath("/"));
            }
        }
        return readDeploymentArtifact(artifact);
    }

    private static Optional<String> readDeploymentArtifact(Path root) throws IOException {
        Path descriptor = root.resolve(BootstrapConstants.DESCRIPTOR_PATH);
        if (!Files.isRegularFile(descriptor)) {
            return Optional.empty();
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(descriptor)) {
            props.load(in);
        }
        return Optional.ofNullable(props.getProperty(BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT))
                .map(ArtifactCoords::fromString)
                .map(coords -> coords.getGroupId() + ":" + coords.getArtifactId());
    }

    private static List<ClassFacts> scanRoot(Path root) throws IOException {
        Indexer indexer = new Indexer();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(ExtensionStructureScanner::isClassFile).toList()) {
                try (InputStream in = Files.newInputStream(file)) {
                    indexer.index(in);
                }
            }
        }
        Index index = indexer.complete();
        List<ClassFacts> facts = new ArrayList<>();
        for (ClassInfo c : index.getKnownClasses()) {
            if (c.nestingType() == ClassInfo.NestingType.TOP_LEVEL && !c.isModule()
                    && !c.name().local().equals("package-info")) {
                facts.add(new ClassFacts(c.name().toString(), !c.annotations(BUILD_STEP).isEmpty(),
                        c.hasDeclaredAnnotation(TARGET_CLASS) || c.interfaceNames().contains(FEATURE)));
            }
        }
        return facts;
    }

    private static boolean isClassFile(Path file) {
        return file.toString().endsWith(".class") && !file.startsWith(file.getRoot().resolve(BootstrapConstants.META_INF))
                && Files.isRegularFile(file);
    }
}
