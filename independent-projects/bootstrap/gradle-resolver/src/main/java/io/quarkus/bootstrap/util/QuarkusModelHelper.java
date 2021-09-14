package io.quarkus.bootstrap.util;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.BootstrapGradleException;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.model.CapabilityContract;
import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.bootstrap.model.gradle.ArtifactCoords;
import io.quarkus.bootstrap.model.gradle.Dependency;
import io.quarkus.bootstrap.model.gradle.QuarkusModel;
import io.quarkus.bootstrap.model.gradle.WorkspaceModule;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

public class QuarkusModelHelper {

    private QuarkusModelHelper() {

    }

    public final static String SERIALIZED_QUARKUS_MODEL = "quarkus-internal.serialized-quarkus-model.path";
    public final static String[] DEVMODE_REQUIRED_TASKS = new String[] { "classes" };
    public final static String[] TEST_REQUIRED_TASKS = new String[] { "classes", "testClasses" };
    public final static List<String> ENABLE_JAR_PACKAGING = Collections
            .singletonList("-Dorg.gradle.java.compile-classpath-packaging=true");

    public static void exportModel(QuarkusModel model, boolean test) throws AppModelResolverException, IOException {
        Path serializedModel = QuarkusModelHelper
                .serializeAppModel(model, test);
        System.setProperty(test ? BootstrapConstants.SERIALIZED_TEST_APP_MODEL : BootstrapConstants.SERIALIZED_APP_MODEL,
                serializedModel.toString());
    }

    public static Path serializeAppModel(QuarkusModel model, boolean test) throws AppModelResolverException, IOException {
        final Path serializedModel = File.createTempFile("quarkus-" + (test ? "test-" : "") + "app-model", ".dat").toPath();
        final ArtifactCoords artifactCoords = model.getWorkspace().getMainModule().getArtifactCoords();
        AppArtifact appArtifact = new AppArtifact(artifactCoords.getGroupId(),
                artifactCoords.getArtifactId(),
                artifactCoords.getVersion());
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(serializedModel))) {
            out.writeObject(QuarkusModelHelper.convert(model, appArtifact));
        }
        return serializedModel;
    }

    public static Path serializeQuarkusModel(QuarkusModel model) throws IOException {
        final Path serializedModel = File.createTempFile("quarkus-model", ".dat").toPath();
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(serializedModel))) {
            out.writeObject(model);
        }
        return serializedModel;
    }

    public static QuarkusModel deserializeQuarkusModel(Path modelPath) throws BootstrapGradleException {
        if (Files.exists(modelPath)) {
            try (InputStream existing = Files.newInputStream(modelPath);
                    ObjectInputStream object = new ObjectInputStream(existing)) {
                QuarkusModel model = (QuarkusModel) object.readObject();
                IoUtils.recursiveDelete(modelPath);
                return model;
            } catch (IOException | ClassNotFoundException e) {
                throw new BootstrapGradleException("Failed to deserialize quarkus model", e);
            }
        }
        throw new BootstrapGradleException("Unable to locate quarkus model");
    }

    public static Path getClassPath(WorkspaceModule model) throws BootstrapGradleException {
        // TODO handle multiple class directory
        final Optional<Path> classDir = model.getSourceSet().getSourceDirectories().stream()
                .filter(File::exists)
                .map(File::toPath)
                .findFirst();
        if (!classDir.isPresent()) {
            throw new BootstrapGradleException("Failed to locate class directory");
        }
        return classDir.get();
    }

    public static AppModel convert(QuarkusModel model, AppArtifact appArtifact) throws AppModelResolverException {
        AppModel.Builder appBuilder = new AppModel.Builder();

        final Map<String, Object> capabilities = new HashMap<>();
        final Map<String, CapabilityContract> capabilitiesContracts = new HashMap<>();
        for (Dependency extensionDependency : model.getDependencies()) {
            boolean extension = false;
            for (File f : extensionDependency.getPaths()) {
                final Path artifactPath = f.toPath();
                if (!Files.exists(artifactPath) || !extensionDependency.getType().equals("jar")) {
                    continue;
                }
                if (Files.isDirectory(artifactPath)) {
                    extension |= processQuarkusDir(extensionDependency,
                            artifactPath.resolve(BootstrapConstants.META_INF),
                            appBuilder, capabilities, capabilitiesContracts);
                } else {
                    try (FileSystem artifactFs = FileSystems.newFileSystem(artifactPath,
                            QuarkusModelHelper.class.getClassLoader())) {
                        extension |= processQuarkusDir(extensionDependency,
                                artifactFs.getPath(BootstrapConstants.META_INF),
                                appBuilder, capabilities, capabilitiesContracts);
                    } catch (IOException e) {
                        throw new AppModelResolverException("Failed to process " + artifactPath, e);
                    }
                }
            }
            appBuilder.addDependency(toAppDependency(extensionDependency, extensionDependency.getFlags(),
                    extension ? AppDependency.RUNTIME_EXTENSION_ARTIFACT_FLAG : 0));
        }
        appBuilder.setCapabilitiesContracts(capabilitiesContracts);

        if (!appArtifact.isResolved()) {
            PathsCollection.Builder paths = PathsCollection.builder();
            WorkspaceModule module = model.getWorkspace().getMainModule();
            module.getSourceSet().getSourceDirectories().stream()
                    .filter(File::exists)
                    .map(File::toPath)
                    .forEach(paths::add);
            module.getSourceSet().getResourceDirectories().stream()
                    .filter(File::exists)
                    .map(File::toPath)
                    .forEach(paths::add);
            appArtifact.setPaths(paths.build());
        }

        for (WorkspaceModule module : model.getWorkspace().getAllModules()) {
            final ArtifactCoords coords = module.getArtifactCoords();
            appBuilder.addLocalProjectArtifact(
                    new AppArtifactKey(coords.getGroupId(), coords.getArtifactId(), null, coords.getType()));
        }

        appBuilder.setAppArtifact(appArtifact)
                .setPlatformImports(model.getPlatformImports());
        return appBuilder.build();
    }

    public static AppDependency toAppDependency(Dependency dependency, int... flags) {
        int allFlags = dependency.getFlags();
        for (int f : flags) {
            allFlags |= f;
        }
        return new AppDependency(toAppArtifact(dependency), "runtime", allFlags);
    }

    private static AppArtifact toAppArtifact(Dependency dependency) {
        AppArtifact artifact = new AppArtifact(dependency.getGroupId(), dependency.getName(), dependency.getClassifier(),
                dependency.getType(), dependency.getVersion());
        artifact.setPaths(QuarkusModelHelper.toPathsCollection(dependency.getPaths()));
        return artifact;
    }

    public static PathsCollection toPathsCollection(Collection<File> files) {
        PathsCollection.Builder paths = PathsCollection.builder();
        for (File f : files) {
            paths.add(f.toPath());
        }
        return paths.build();
    }

    public static Properties resolveDescriptor(final Path path) {
        final Properties rtProps;
        if (!Files.exists(path)) {
            // not a platform artifact
            return null;
        }
        rtProps = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            rtProps.load(reader);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load extension description " + path, e);
        }
        return rtProps;
    }

    private static boolean processQuarkusDir(Dependency d, Path quarkusDir, AppModel.Builder appBuilder,
            Map<String, Object> capabilities, Map<String, CapabilityContract> capabilitiesContracts) {
        if (!Files.exists(quarkusDir)) {
            return false;
        }
        final Path quarkusDescr = quarkusDir.resolve(BootstrapConstants.DESCRIPTOR_FILE_NAME);
        if (!Files.exists(quarkusDescr)) {
            return false;
        }
        final Properties extProps = QuarkusModelHelper.resolveDescriptor(quarkusDescr);
        if (extProps == null) {
            return false;
        }
        final String extensionCoords = d.getGroupId() + ":" + d.getName() + ":"
                + (d.getClassifier() == null ? "" : d.getClassifier()) + ":"
                + (d.getType() == null || d.getType().isEmpty() ? "jar" : d.getType()) + ":" + d.getVersion();
        appBuilder.handleExtensionProperties(extProps, extensionCoords);

        final String providesCapabilities = extProps.getProperty(BootstrapConstants.PROP_PROVIDES_CAPABILITIES);
        if (providesCapabilities != null) {
            capabilitiesContracts.put(extensionCoords,
                    CapabilityContract.providesCapabilities(extensionCoords, providesCapabilities));
        }
        return true;
    }

    static AppDependency alignVersion(AppDependency dependency, Map<AppArtifactKey, AppDependency> versionMap) {
        AppArtifactKey appKey = new AppArtifactKey(dependency.getArtifact().getGroupId(),
                dependency.getArtifact().getArtifactId());
        if (versionMap.containsKey(appKey)) {
            return versionMap.get(appKey);
        }
        return dependency;
    }

}
