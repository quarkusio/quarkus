package io.quarkus.bootstrap.util;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.CapabilityContract;
import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.GACT;
import io.quarkus.maven.dependency.ResolvedDependency;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BootstrapUtils {

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

    public static AppModel convert(ApplicationModel appModel) {
        if (appModel instanceof AppModel) {
            return (AppModel) appModel;
        }
        final AppModel.Builder builder = new AppModel.Builder();
        final ResolvedDependency resolvedArtifact = appModel.getAppArtifact();
        final AppArtifact appArtifact = new AppArtifact(resolvedArtifact.getGroupId(), resolvedArtifact.getArtifactId(),
                resolvedArtifact.getClassifier(),
                resolvedArtifact.getType(), resolvedArtifact.getVersion(), appModel.getApplicationModule(),
                resolvedArtifact.getScope(), resolvedArtifact.getFlags());
        if (appModel.getAppArtifact().isResolved()) {
            appArtifact.setPaths(PathsCollection.from(appModel.getAppArtifact().getResolvedPaths()));
        }
        builder.setAppArtifact(appArtifact);
        builder.setCapabilitiesContracts(appModel.getExtensionCapabilities().stream()
                .map(c -> new CapabilityContract(c.getExtension(), new ArrayList<>(c.getProvidesCapabilities())))
                .collect(Collectors.toMap(CapabilityContract::getExtension, Function.identity())));
        builder.setPlatformImports(appModel.getPlatforms());
        appModel.getDependencies().forEach(d -> {
            final AppArtifact a = new AppArtifact(d.getGroupId(), d.getArtifactId(), d.getClassifier(), d.getType(),
                    d.getVersion(), d.getWorkspaceModule(), d.getScope(), d.getFlags());
            a.setPaths(d.getResolvedPaths() == null ? PathsCollection.of()
                    : PathsCollection.from(d.getResolvedPaths()));
            builder.addDependency(new AppDependency(a, d.getScope(), d.getFlags()));
        });
        appModel.getLowerPriorityArtifacts().forEach(k -> builder.addLesserPriorityArtifact(
                new AppArtifactKey(k.getGroupId(), k.getArtifactId(), k.getClassifier(), k.getType())));
        appModel.getParentFirst().forEach(k -> builder
                .addParentFirstArtifact(new AppArtifactKey(k.getGroupId(), k.getArtifactId(), k.getClassifier(), k.getType())));
        appModel.getRunnerParentFirst().forEach(k -> builder.addRunnerParentFirstArtifact(
                new AppArtifactKey(k.getGroupId(), k.getArtifactId(), k.getClassifier(), k.getType())));

        appModel.getReloadableWorkspaceDependencies().forEach(k -> builder.addLocalProjectArtifact(
                new AppArtifactKey(k.getGroupId(), k.getArtifactId())));

        return builder.build();
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

    public static Path resolveSerializedAppModelPath(Path projectBuildDir) {
        return projectBuildDir.resolve("quarkus").resolve("bootstrap").resolve("dev-app-model.dat");
    }
}
