package io.quarkus.bootstrap.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.GACT;
import io.quarkus.maven.dependency.ResolvedDependency;

public class BootstrapUtils {

    private static final Logger log = Logger.getLogger(BootstrapUtils.class);

    private static final int CP_CACHE_FORMAT_ID = 2;

    private static Pattern splitByWs;

    public static String[] splitByWhitespace(String s) {
        if (s == null) {
            return null;
        }
        if (splitByWs == null) {
            splitByWs = Pattern.compile("\\s+");
        }
        return splitByWs.split(s);
    }

    public static ArtifactKey[] parseDependencyCondition(String s) {
        final String[] strArr = splitByWhitespace(s);
        if (strArr == null) {
            return null;
        }
        final ArtifactKey[] keys = new ArtifactKey[strArr.length];
        for (int i = 0; i < strArr.length; ++i) {
            keys[i] = GACT.fromString(strArr[i]);
        }
        return keys;
    }

    public static void exportModel(ApplicationModel model, boolean test) throws AppModelResolverException, IOException {
        Path serializedModel = serializeAppModel(model, test);
        System.setProperty(test ? BootstrapConstants.SERIALIZED_TEST_APP_MODEL : BootstrapConstants.SERIALIZED_APP_MODEL,
                serializedModel.toString());
    }

    public static Path serializeAppModel(ApplicationModel model, boolean test) throws AppModelResolverException, IOException {
        final Path serializedModel = File.createTempFile("quarkus-" + (test ? "test-" : "") + "app-model", ".dat").toPath();
        serializeAppModel(model, serializedModel);
        return serializedModel;
    }

    public static void serializeAppModel(ApplicationModel model, final Path serializedModel)
            throws IOException {
        Files.createDirectories(serializedModel.getParent());
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(serializedModel))) {
            out.writeObject(model);
        }
    }

    public static Path serializeQuarkusModel(ApplicationModel model) throws IOException {
        final Path serializedModel = File.createTempFile("quarkus-model", ".dat").toPath();
        serializeAppModel(model, serializedModel);
        return serializedModel;
    }

    public static ApplicationModel deserializeQuarkusModel(Path modelPath) throws AppModelResolverException {
        if (Files.exists(modelPath)) {
            try (InputStream existing = Files.newInputStream(modelPath);
                    ObjectInputStream object = new ObjectInputStream(existing)) {
                ApplicationModel model = (ApplicationModel) object.readObject();
                IoUtils.recursiveDelete(modelPath);
                return model;
            } catch (IOException | ClassNotFoundException e) {
                throw new AppModelResolverException("Failed to deserialize quarkus model", e);
            }
        }
        throw new AppModelResolverException("Unable to locate quarkus model");
    }

    /**
     * Returns a location where a serialized {@link ApplicationModel} would be found for dev mode.
     *
     * @param projectBuildDir project build directory
     * @return file of a serialized application model for dev mode
     */
    public static Path resolveSerializedAppModelPath(Path projectBuildDir) {
        return getBootstrapBuildDir(projectBuildDir).resolve("dev-app-model.dat");
    }

    /**
     * Returns a location where a serialized {@link ApplicationModel} would be found for test mode.
     *
     * @param projectBuildDir project build directory
     * @return file of a serialized application model for test mode
     */
    public static Path getSerializedTestAppModelPath(Path projectBuildDir) {
        return getBootstrapBuildDir(projectBuildDir).resolve("test-app-model.dat");
    }

    private static Path getBootstrapBuildDir(Path projectBuildDir) {
        return projectBuildDir.resolve("quarkus").resolve("bootstrap");
    }

    /**
     * Serializes an {@link ApplicationModel} along with the workspace ID for which it was resolved.
     * The serialization format will be different from the one used by {@link #resolveSerializedAppModelPath(Path)}
     * and {@link #getSerializedTestAppModelPath(Path)}.
     *
     * @param appModel application model to serialize
     * @param workspaceId workspace ID
     * @param file target file
     * @throws IOException in case of an IO failure
     */
    public static void writeAppModelWithWorkspaceId(ApplicationModel appModel, int workspaceId, Path file) throws IOException {
        Files.createDirectories(file.getParent());
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(file))) {
            out.writeInt(CP_CACHE_FORMAT_ID);
            out.writeInt(workspaceId);
            out.writeObject(appModel);
        }
        log.debugf("Serialized application model to %s", file);
    }

    /**
     * Deserializes an {@link ApplicationModel} from a file.
     * <p>
     * The implementation will check whether the serialization format of the file matches the expected one.
     * If it does not, the method will return null even if the file exists.
     * <p>
     * The implementation will compare the deserialized workspace ID to the argument {@code workspaceId}
     * and if they don't match the method will return null.
     * <p>
     * Once the {@link ApplicationModel} was deserialized, the dependency paths will be checked for existence.
     * If a dependency path does not exist, the method will throw an exception.
     *
     * @param file serialized application model file
     * @param workspaceId expected workspace ID
     * @return deserialized application model
     * @throws ClassNotFoundException in case a required class could not be loaded
     * @throws IOException in case of an IO failure
     */
    public static ApplicationModel readAppModelWithWorkspaceId(Path file, int workspaceId)
            throws ClassNotFoundException, IOException {
        try (ObjectInputStream reader = new ObjectInputStream(Files.newInputStream(file))) {
            if (reader.readInt() == CP_CACHE_FORMAT_ID) {
                if (reader.readInt() == workspaceId) {
                    final ApplicationModel appModel = (ApplicationModel) reader.readObject();
                    log.debugf("Loaded application model %s from %s", appModel, file);
                    for (ResolvedDependency d : appModel.getDependencies(DependencyFlags.DEPLOYMENT_CP)) {
                        for (Path p : d.getResolvedPaths()) {
                            if (!Files.exists(p)) {
                                throw new IOException("Cached artifact does not exist: " + p);
                            }
                        }
                    }
                    return appModel;
                } else {
                    log.debugf("Application model saved in %s has a different workspace ID", file);
                }
            } else {
                log.debugf("Unsupported application model serialization format in %s", file);
            }
        }
        return null;
    }
}
