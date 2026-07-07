package io.quarkus.gradle.application.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.bootstrap.model.MutableJarApplicationModel;
import io.quarkus.deployment.dev.remotedev.RemoteDevPackageChange;
import io.quarkus.deployment.dev.remotedev.RemoteDevPackageClient;
import io.quarkus.deployment.dev.remotedev.RemoteDevPackageClientConfig;
import io.quarkus.deployment.dev.remotedev.RemoteDevPackageClientFactory;
import io.quarkus.deployment.dev.remotedev.RemoteDevPackageClientResult;
import io.quarkus.deployment.dev.remotedev.RemoteDevPackageDiff;
import io.quarkus.deployment.dev.remotedev.RemoteDevPackageReconnectListener;
import io.quarkus.gradle.application.internal.packaging.PackageResult;
import io.quarkus.gradle.application.internal.packaging.PackageResultCodec;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import io.quarkus.paths.PathList;

class QuarkusApplicationRemoteDevTaskTest {

    @TempDir
    Path directory;

    @Test
    void rejectsInvocationWithoutContinuousBuildBeforeReadingPackageResult() {
        QuarkusApplicationRemoteDevTask task = task();
        task.getContinuousBuild().set(false);

        assertThatThrownBy(task::runRemoteDev)
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("--continuous");
    }

    @Test
    void rejectsNonMutablePackageResultBeforeConnecting() throws Exception {
        QuarkusApplicationRemoteDevTask task = task();
        task.getContinuousBuild().set(true);
        Path outputRoot = Files.createDirectories(directory.resolve("package"));
        Path receipt = directory.resolve("package-result.properties");
        new PackageResultCodec().write(receipt, new PackageResult(
                "fast",
                QuarkusApplicationBuildType.FAST_JAR,
                outputRoot,
                "fast",
                outputRoot.resolve("quarkus-run.jar"),
                Optional.empty(),
                Optional.empty(),
                false,
                false,
                Optional.empty(),
                List.of()));
        task.getPackageResultFile().set(receipt.toFile());
        task.getPackageOutputDirectory().set(outputRoot.toFile());

        assertThatThrownBy(task::runRemoteDev)
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("requires a mutable-jar package result");
    }

    @Test
    void reusesSessionForIncrementalDeliveryUsingCurrentSnapshot() throws Exception {
        TestRemoteDevClientFactory clientFactory = new TestRemoteDevClientFactory();
        QuarkusApplicationRemoteDevTask task = task(clientFactory);
        task.getContinuousBuild().set(true);
        task.liveReloadUrl("http://127.0.0.1:19120/");
        task.liveReloadPassword("secret");
        Path outputRoot = Files.createDirectories(directory.resolve("package"));
        Files.createDirectories(outputRoot.resolve("lib"));
        Files.writeString(outputRoot.resolve("lib/application.jar"), "one");
        Path receipt = directory.resolve("package-result.properties");
        new PackageResultCodec().write(receipt, new PackageResult(
                "mutable",
                QuarkusApplicationBuildType.MUTABLE_JAR,
                outputRoot,
                "mutable",
                outputRoot.resolve("quarkus-run.jar"),
                Optional.empty(),
                Optional.empty(),
                true,
                false,
                Optional.empty(),
                List.of()));
        task.getPackageResultFile().set(receipt.toFile());
        task.getPackageOutputDirectory().set(outputRoot.toFile());
        clientFactory.requestedPaths.add(Set.of("lib/application.jar"));

        task.runRemoteDev();

        Files.writeString(outputRoot.resolve("lib/application.jar"), "two");

        task.runRemoteDev();

        assertThat(clientFactory.clients).hasSize(1);
        assertThat(clientFactory.clients.get(0).sentChanges)
                .containsExactly("lib/application.jar", "lib/application.jar");
        assertThat(clientFactory.clients.get(0).connected).isEqualTo(1);
        assertThat(clientFactory.clients.get(0).sent).isEqualTo(2);
        assertThat(Files.readString(task.getReceiptFile().get().getAsFile().toPath()))
                .contains("sequence=2")
                .contains("outcome=SENT");
    }

    @Test
    void materializesDevModeApplicationFilesBeforeConnecting() throws Exception {
        TestRemoteDevClientFactory clientFactory = new TestRemoteDevClientFactory();
        QuarkusApplicationRemoteDevTask task = task(clientFactory);
        task.getContinuousBuild().set(true);
        task.liveReloadUrl("http://127.0.0.1:19120/");
        task.liveReloadPassword("secret");
        Path outputRoot = Files.createDirectories(directory.resolve("package"));
        Path appJar = outputRoot.resolve("app/test-app.jar");
        writeJar(appJar, Map.of("org/acme/App.class", "class-bytes"));
        Path appModel = outputRoot.resolve("lib/deployment/appmodel.dat");
        Files.createDirectories(appModel.getParent());
        writeAppModel(appModel, appJar);
        Path receipt = directory.resolve("package-result.properties");
        new PackageResultCodec().write(receipt, new PackageResult(
                "mutable",
                QuarkusApplicationBuildType.MUTABLE_JAR,
                outputRoot,
                "mutable",
                outputRoot.resolve("quarkus-run.jar"),
                Optional.empty(),
                Optional.empty(),
                true,
                false,
                Optional.empty(),
                List.of()));
        task.getPackageResultFile().set(receipt.toFile());
        task.getPackageOutputDirectory().set(outputRoot.toFile());
        clientFactory.requestedPaths.add(Set.of("dev/app/org/acme/App.class"));

        task.runRemoteDev();

        assertThat(outputRoot.resolve("dev/app/org/acme/App.class")).isRegularFile();
        assertThat(clientFactory.clients).hasSize(1);
        assertThat(clientFactory.clients.get(0).connectedHashes).containsKey("dev/app/org/acme/App.class");
        assertThat(clientFactory.clients.get(0).sentChanges).containsExactly("dev/app/org/acme/App.class");
    }

    private QuarkusApplicationRemoteDevTask task() {
        return task(new TestRemoteDevClientFactory());
    }

    private static void writeJar(Path jar, Map<String, String> entries) throws IOException {
        Files.createDirectories(jar.getParent());
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                output.putNextEntry(new JarEntry(entry.getKey()));
                output.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                output.closeEntry();
            }
        }
    }

    private static void writeAppModel(Path appModel, Path appJar) throws IOException {
        ArtifactKey appKey = ArtifactKey.of("org.acme", "test-app", "", "jar");
        var appArtifact = ResolvedDependencyBuilder.newInstance()
                .setGroupId(appKey.getGroupId())
                .setArtifactId(appKey.getArtifactId())
                .setType(appKey.getType())
                .setVersion("1.0")
                .setResolvedPaths(PathList.of(appJar));
        var applicationModel = new ApplicationModelBuilder()
                .setAppArtifact(appArtifact)
                .addReloadableWorkspaceModule(appKey)
                .build();
        var mutableModel = new MutableJarApplicationModel("test-app", new HashMap<>(), applicationModel, null,
                "app/test-app.jar");
        try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(appModel))) {
            output.writeObject(mutableModel);
        }
    }

    private QuarkusApplicationRemoteDevTask task(TestRemoteDevClientFactory clientFactory) {
        Project project = ProjectBuilder.builder().build();
        TestRemoteDevTask task = project.getTasks()
                .register("remoteDev", TestRemoteDevTask.class)
                .get();
        task.clientFactory = clientFactory;
        task.getBuildName().set("mutable");
        task.getBuildType().set(QuarkusApplicationBuildType.MUTABLE_JAR);
        task.getOutputName().set("mutable");
        task.getOutputDirectory().set(directory.resolve("package").toFile());
        task.getProjectDirectory().set(directory.toFile());
        task.getQuarkusBuildProperties().set(Map.of());
        task.getPackageResultFile().set(directory.resolve("missing.properties").toFile());
        task.getPackageOutputDirectory().set(directory.resolve("package").toFile());
        task.getReceiptFile().set(directory.resolve("remote-dev-result.properties").toFile());
        task.getPackageSnapshotFile().set(directory.resolve("package-snapshot.tsv").toFile());
        task.getCloseReceiptFile().set(directory.resolve("session-closed.txt").toFile());
        task.getReconnectTriggerFile().set(directory.resolve("reconnect.trigger").toFile());
        return task;
    }

    public abstract static class TestRemoteDevTask extends QuarkusApplicationRemoteDevTask {
        private RemoteDevPackageClientFactory clientFactory = config -> {
            throw new IOException("Unexpected remote-dev client creation for " + config.redactedRemoteUrl());
        };

        @Override
        protected RemoteDevPackageClientFactory clientFactory() {
            return clientFactory;
        }
    }

    private static final class TestRemoteDevClientFactory implements RemoteDevPackageClientFactory {
        private final List<Set<String>> requestedPaths = new ArrayList<>();
        private final List<TestRemoteDevClient> clients = new ArrayList<>();

        @Override
        public RemoteDevPackageClient create(RemoteDevPackageClientConfig config) {
            TestRemoteDevClient client = new TestRemoteDevClient(requestedPaths.remove(0));
            clients.add(client);
            return client;
        }
    }

    private static final class TestRemoteDevClient implements RemoteDevPackageClient {
        private final Set<String> requestedPaths;
        private Map<String, String> connectedHashes = Map.of();
        private final List<String> sentChanges = new ArrayList<>();
        private int connected;
        private int sent;

        private TestRemoteDevClient(Set<String> requestedPaths) {
            this.requestedPaths = requestedPaths;
        }

        @Override
        public RemoteDevPackageClientResult connect(Map<String, String> localHashes) {
            connected++;
            connectedHashes = new HashMap<>(localHashes);
            return RemoteDevPackageClientResult.connected(requestedPaths);
        }

        @Override
        public RemoteDevPackageClientResult send(RemoteDevPackageDiff diff) {
            sent++;
            sentChanges.addAll(diff.changed().stream()
                    .map(RemoteDevPackageChange::relativePath)
                    .toList());
            return RemoteDevPackageClientResult.sent(diff.changed().size(), diff.deleted().size());
        }

        @Override
        public void startChangePolling(RemoteDevPackageReconnectListener listener) {
        }

        @Override
        public void close() {
        }
    }
}
