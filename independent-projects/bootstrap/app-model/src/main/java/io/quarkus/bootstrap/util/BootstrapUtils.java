package io.quarkus.bootstrap.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.CapabilityContract;
import io.quarkus.bootstrap.model.DefaultApplicationModel;
import io.quarkus.bootstrap.model.ExtensionCapabilities;
import io.quarkus.bootstrap.model.JvmOptions;
import io.quarkus.bootstrap.model.JvmOptionsBuilder;
import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.bootstrap.model.PlatformImports;
import io.quarkus.bootstrap.model.PlatformImportsImpl;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.workspace.ArtifactSources;
import io.quarkus.bootstrap.workspace.DefaultArtifactSources;
import io.quarkus.bootstrap.workspace.DefaultWorkspaceModule;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.GACT;
import io.quarkus.maven.dependency.GACTV;
import io.quarkus.maven.dependency.ResolvedArtifactDependency;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import io.quarkus.paths.PathCollection;

public class BootstrapUtils {

    private static final Logger log = Logger.getLogger(BootstrapUtils.class);
    private static final int CP_CACHE_FORMAT_ID = 2;
    /*
     * We use JSON when serializing the application model in the build system for later use by tests. This allows Develocity's
     * test distribution feature to read the model and replace the file paths inside with the paths on the remote agent, which
     * will be different from the ones on the host that started the build.
     *
     * The JSON mapper is configured to be as close to Java serialization as possible, to keep the changes to the model
     * classes minimal.
     */
    private static final JsonMapper MAPPER;

    static {
        JsonMapper.Builder objectMapper = JsonMapper.builder();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.disable(MapperFeature.AUTO_DETECT_GETTERS, MapperFeature.AUTO_DETECT_IS_GETTERS);
        objectMapper.visibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        SimpleModule module = new SimpleModule();
        module.addAbstractTypeMapping(ApplicationModel.class, DefaultApplicationModel.class);
        module.addAbstractTypeMapping(PathCollection.class, PathsCollection.class);
        module.addAbstractTypeMapping(WorkspaceModule.class, DefaultWorkspaceModule.class);
        module.addAbstractTypeMapping(ArtifactSources.class, DefaultArtifactSources.class);
        module.addAbstractTypeMapping(Dependency.class, ResolvedArtifactDependency.class);
        module.addAbstractTypeMapping(ResolvedDependency.class, ResolvedArtifactDependency.class);
        module.addAbstractTypeMapping(ArtifactCoords.class, GACTV.class);
        module.addAbstractTypeMapping(PlatformImports.class, PlatformImportsImpl.class);
        module.addAbstractTypeMapping(ExtensionCapabilities.class, CapabilityContract.class);
        module.addAbstractTypeMapping(ArtifactKey.class, GACT.class);
        module.addAbstractTypeMapping(JvmOptions.class, JvmOptionsBuilder.JvmOptionsImpl.class);
        module.addSerializer(ResolvedDependencyBuilder.class, new StdSerializer<>(ResolvedDependencyBuilder.class) {
            @Override
            public void serialize(ResolvedDependencyBuilder value, JsonGenerator gen, SerializerProvider provider)
                    throws IOException {
                gen.writeObject(value.build());
            }
        });
        module.addSerializer(Path.class, new StdSerializer<>(Path.class) {
            @Override
            public void serialize(Path value, JsonGenerator gen, SerializerProvider provider) throws IOException {
                gen.writeString(value.toString());
            }
        });
        module.addDeserializer(Path.class, new StdDeserializer<>(Path.class) {
            @Override
            public Path deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String value = p.getText();
                return value == null ? null : Path.of(value);
            }
        });
        objectMapper.addModule(module);
        objectMapper.addModule(new ParameterNamesModule());
        MAPPER = objectMapper.build();
    }

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

    public static void serializeAppModel(ApplicationModel model, final Path serializedModel) throws IOException {
        if (Proxy.isProxyClass(model.getClass())) {
            /*
             * For Gradle Tooling API proxies, we need to call the serialization code inside its own classloader,
             * because our Jackson Mapper can't serialize the proxy.
             */
            model.serializeTo(serializedModel);
        } else {
            Files.createDirectories(serializedModel.getParent());
            MAPPER.writeValue(serializedModel.toFile(), model);
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
                    InputStreamReader reader = new InputStreamReader(existing, StandardCharsets.UTF_8)) {
                return MAPPER.readValue(reader, DefaultApplicationModel.class);
            } catch (IOException e) {
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

    /**
     * Generates a comma-separated list of flag names for an integer representation of the flags.
     *
     * @param flags flags as an integer value
     * @return comma-separated list of the flag names
     */
    public static String toTextFlags(int flags) {
        var sb = new StringBuilder();
        appendFlagIfSet(sb, flags, DependencyFlags.OPTIONAL, "optional");
        appendFlagIfSet(sb, flags, DependencyFlags.DIRECT, "direct");
        appendFlagIfSet(sb, flags, DependencyFlags.RUNTIME_CP, "runtime-cp");
        appendFlagIfSet(sb, flags, DependencyFlags.DEPLOYMENT_CP, "deployment-cp");
        appendFlagIfSet(sb, flags, DependencyFlags.RUNTIME_EXTENSION_ARTIFACT, "runtime-extension-artifact");
        appendFlagIfSet(sb, flags, DependencyFlags.WORKSPACE_MODULE, "workspace-module");
        appendFlagIfSet(sb, flags, DependencyFlags.RELOADABLE, "reloadable");
        appendFlagIfSet(sb, flags, DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT,
                "top-level-runtime-extension-artifact");
        appendFlagIfSet(sb, flags, DependencyFlags.CLASSLOADER_PARENT_FIRST, "classloader-parent-first");
        appendFlagIfSet(sb, flags, DependencyFlags.CLASSLOADER_RUNNER_PARENT_FIRST, "classloader-runner-parent-first");
        appendFlagIfSet(sb, flags, DependencyFlags.CLASSLOADER_LESSER_PRIORITY, "classloader-lesser-priority");
        appendFlagIfSet(sb, flags, DependencyFlags.COMPILE_ONLY, "compile-only");
        appendFlagIfSet(sb, flags, DependencyFlags.VISITED, "visited");
        return sb.toString();
    }

    private static void appendFlagIfSet(StringBuilder sb, int flags, int flagValue, String flagName) {
        if ((flags & flagValue) == flagValue) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(flagName);
        }
    }
}
