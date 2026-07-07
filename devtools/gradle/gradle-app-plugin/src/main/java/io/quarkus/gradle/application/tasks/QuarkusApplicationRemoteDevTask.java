package io.quarkus.gradle.application.tasks;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.LocalState;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;
import org.gradle.deployment.internal.DeploymentRegistry;
import org.gradle.work.DisableCachingByDefault;

import io.quarkus.bootstrap.model.MutableJarApplicationModel;
import io.quarkus.deployment.dev.remotedev.HttpRemoteDevPackageClient;
import io.quarkus.deployment.dev.remotedev.RemoteDevPackageClientConfig;
import io.quarkus.deployment.dev.remotedev.RemoteDevPackageClientFactory;
import io.quarkus.deployment.dev.remotedev.RemoteDevPackageDiff;
import io.quarkus.deployment.dev.remotedev.RemoteDevPackageSnapshot;
import io.quarkus.deployment.mutability.DevModeTask;
import io.quarkus.gradle.application.internal.config.EffectiveConfigPlan;
import io.quarkus.gradle.application.internal.config.EffectiveConfigPlanner;
import io.quarkus.gradle.application.internal.config.EffectiveConfigRequest;
import io.quarkus.gradle.application.internal.packaging.PackageResult;
import io.quarkus.gradle.application.internal.packaging.PackageResultCodec;
import io.quarkus.gradle.application.internal.remotedev.QuarkusApplicationRemoteDevDeployments;
import io.quarkus.gradle.application.internal.remotedev.QuarkusApplicationRemoteDevSession;

/**
 * Synchronizes a locally built mutable-JAR package with a remote Quarkus development application.
 * <p>
 * The plugin registers this task as {@code quarkusApplicationRemoteDev}. It must be invoked with Gradle's
 * {@code --continuous} option. The task builds the plugin-owned mutable-JAR package, retains a session in Gradle's
 * deployment registry, snapshots successful package outputs, and sends requested changes to the remote endpoint.
 * Command-line URL and password options override values resolved from Quarkus configuration for that invocation.
 * <p>
 * The task is neither configuration-cache compatible nor build-cacheable because it owns a long-lived connection,
 * mutable local state, and remote application mutations. The supported compatibility contract covers the
 * plugin-registered task and its documented properties and options. No compatibility commitment is made for direct
 * construction, additional registration, or subclassing.
 */
@DisableCachingByDefault(because = "Gradle-native remote dev is long-lived and mutates a remote application")
public abstract class QuarkusApplicationRemoteDevTask extends QuarkusApplicationLaunchTask {

    private static final int LOGGED_PATH_LIMIT = 50;

    private transient String liveReloadUrl;
    private transient String liveReloadPassword;

    /**
     * Creates a remote-development task with empty task-specific build properties that always executes.
     */
    public QuarkusApplicationRemoteDevTask() {
        getQuarkusBuildProperties().convention(Map.of());
        getOutputs().upToDateWhen(task -> false);
    }

    /**
     * Returns whether this invocation uses Gradle continuous build.
     *
     * @return whether Gradle was invoked with {@code --continuous}
     */
    @Input
    public abstract Property<Boolean> getContinuousBuild();

    /**
     * Returns Quarkus properties assembled from the root and remote-development DSL.
     *
     * @return the Quarkus build properties
     */
    @Input
    public abstract MapProperty<String, String> getQuarkusBuildProperties();

    /**
     * Returns the project directory used to identify the retained session.
     *
     * @return the project directory
     */
    @Internal
    public abstract DirectoryProperty getProjectDirectory();

    /**
     * Returns the result descriptor for the plugin-owned mutable-JAR package.
     *
     * @return the package result file
     */
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getPackageResultFile();

    /**
     * Returns the mutable-JAR package directory synchronized with the remote application.
     *
     * @return the package output directory
     */
    @Internal
    public abstract DirectoryProperty getPackageOutputDirectory();

    /**
     * Returns the per-iteration delivery receipt.
     *
     * @return the delivery receipt
     */
    @OutputFile
    public abstract RegularFileProperty getReceiptFile();

    /**
     * Returns local state containing the last successfully delivered package snapshot.
     *
     * @return the package snapshot file
     */
    @LocalState
    public abstract RegularFileProperty getPackageSnapshotFile();

    /**
     * Returns local state written when the retained remote-development session closes.
     *
     * @return the close receipt
     */
    @LocalState
    public abstract RegularFileProperty getCloseReceiptFile();

    /**
     * Returns the trigger whose changes request reconnection even when package outputs are unchanged.
     *
     * @return the reconnect trigger file
     */
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getReconnectTriggerFile();

    /**
     * Returns Gradle's deployment registry used to retain the remote-development session.
     *
     * @return the deployment registry
     */
    @Inject
    public abstract DeploymentRegistry getDeploymentRegistry();

    /**
     * Overrides {@code quarkus.live-reload.url} for this invocation.
     *
     * @param liveReloadUrl the remote live-reload URL
     */
    @Option(description = "Remote Quarkus live-reload URL", option = "live-reload-url")
    public void liveReloadUrl(String liveReloadUrl) {
        this.liveReloadUrl = liveReloadUrl;
    }

    /**
     * Overrides {@code quarkus.live-reload.password} for this invocation.
     * The value is transient task state and is not written to modeled task inputs or receipts.
     *
     * @param liveReloadPassword the remote live-reload password
     */
    @Option(description = "Remote Quarkus live-reload password", option = "live-reload-password")
    public void liveReloadPassword(String liveReloadPassword) {
        this.liveReloadPassword = liveReloadPassword;
    }

    /**
     * Starts or reuses the remote session and delivers the current package changes requested by the server.
     *
     * @throws IOException when package snapshots or receipts cannot be read or written
     */
    @TaskAction
    public void runRemoteDev() throws IOException {
        warnIfLegacyAmbientConfigCaptureEnabled();
        validateContinuousBuild();
        PackageResult packageResult = new PackageResultCodec().read(getPackageResultFile().get().getAsFile().toPath());
        if (!packageResult.mutable()) {
            throw new GradleException("Task '" + getPath() + "' requires a mutable-jar package result, but '"
                    + packageResult.buildName() + "' produced '" + packageResult.buildType() + "'.");
        }
        Path packageRoot = packageRoot(packageResult);
        Map<String, String> remoteConfig = remoteConfig();
        URI remoteUrl = remoteUrl(remoteConfig);
        Optional<String> password = Optional.ofNullable(remoteConfig.get("quarkus.live-reload.password"));
        var clientConfig = new RemoteDevPackageClientConfig(remoteUrl, password);
        String configFingerprint = QuarkusApplicationRemoteDevDeployments.configFingerprint(
                getPackageResultFile().get().getAsFile().toPath(), packageRoot, clientConfig.redactedRemoteUrl());
        String deploymentId = QuarkusApplicationRemoteDevDeployments.deploymentId(
                getProjectDirectory().get().getAsFile().toPath(), getPath(), getBuildName().get(), configFingerprint);
        materializeRemoteDevPackage(packageRoot);
        RemoteDevPackageSnapshot current = RemoteDevPackageSnapshot.capture(packageRoot);
        RemoteDevPackageSnapshot previous = RemoteDevPackageSnapshot
                .read(getPackageSnapshotFile().get().getAsFile().toPath());
        RemoteDevPackageDiff localDiff = current.diffSince(previous, packageRoot);
        logLocalDiff(localDiff, previous.isEmpty());
        var acquired = QuarkusApplicationRemoteDevDeployments.getOrStart(getDeploymentRegistry(), deploymentId,
                new QuarkusApplicationRemoteDevDeployments.Parameters(
                        getCloseReceiptFile().get().getAsFile().toPath(),
                        getReconnectTriggerFile().get().getAsFile().toPath()));
        QuarkusApplicationRemoteDevSession session = acquired.session();
        long sequence = session.nextSequence();
        QuarkusApplicationRemoteDevSession.DeliveryResult delivered = session.deliver(current, packageRoot,
                getPackageSnapshotFile().get().getAsFile().toPath(), () -> clientFactory().create(clientConfig));
        if (delivered.outcome().equals("SENT")) {
            logRemoteSend(localDiff);
        }
        if (delivered.requestedBatches().isEmpty() && delivered.outcome().equals("CONNECTED")) {
            logRemoteRequest(new RemoteDevPackageDiff(List.of(), List.of()));
        } else {
            delivered.requestedBatches().forEach(this::logRemoteRequest);
        }
        writeReceipt(sequence, delivered.outcome(), delivered.changed(), delivered.deleted(), acquired.started(), packageRoot);
    }

    /**
     * Returns the client factory used to connect to the remote live-reload endpoint.
     *
     * @return the remote-development client factory
     */
    protected RemoteDevPackageClientFactory clientFactory() {
        return HttpRemoteDevPackageClient::new;
    }

    private void materializeRemoteDevPackage(Path packageRoot) throws IOException {
        Path appModel = packageRoot.resolve("lib/deployment/appmodel.dat");
        if (!Files.exists(appModel)) {
            return;
        }
        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(appModel))) {
            MutableJarApplicationModel model = (MutableJarApplicationModel) input.readObject();
            DevModeTask.extractDevModeClasses(packageRoot, model.getAppModel(packageRoot), null);
        } catch (ClassNotFoundException e) {
            throw new GradleException("Failed to read mutable-jar application model from " + appModel, e);
        }
    }

    private void logLocalDiff(RemoteDevPackageDiff localDiff, boolean initialSnapshot) {
        if (localDiff.isEmpty()) {
            getLogger().lifecycle("Quarkus remote dev package snapshot has no local changes.");
            return;
        }
        getLogger().lifecycle("Quarkus remote dev package snapshot {}: {} added/updated, {} deleted.",
                initialSnapshot ? "initialized" : "changed", localDiff.changed().size(), localDiff.deleted().size());
        logChangedPaths("Quarkus remote dev local update", localDiff);
        logDeletedPaths("Quarkus remote dev local delete", localDiff);
    }

    private void logRemoteRequest(RemoteDevPackageDiff requested) {
        if (requested.isEmpty()) {
            getLogger().lifecycle("Quarkus remote dev server requested no package files.");
            return;
        }
        getLogger().lifecycle("Quarkus remote dev server requested {} package files.", requested.changed().size());
        logChangedPaths("Quarkus remote dev uploading", requested);
    }

    private void logRemoteSend(RemoteDevPackageDiff diff) {
        if (diff.isEmpty()) {
            getLogger().lifecycle("Quarkus remote dev has no package files to send.");
            return;
        }
        getLogger().lifecycle("Quarkus remote dev sending {} added/updated and {} deleted package files.",
                diff.changed().size(), diff.deleted().size());
        logChangedPaths("Quarkus remote dev uploading", diff);
        logDeletedPaths("Quarkus remote dev deleting", diff);
    }

    private void logChangedPaths(String prefix, RemoteDevPackageDiff diff) {
        diff.changed().stream()
                .limit(LOGGED_PATH_LIMIT)
                .forEach(change -> getLogger().lifecycle("{} {}", prefix, change.relativePath()));
        int remaining = diff.changed().size() - LOGGED_PATH_LIMIT;
        if (remaining > 0) {
            getLogger().lifecycle("{} ... and {} more files", prefix, remaining);
        }
    }

    private void logDeletedPaths(String prefix, RemoteDevPackageDiff diff) {
        diff.deleted().stream()
                .limit(LOGGED_PATH_LIMIT)
                .forEach(path -> getLogger().lifecycle("{} {}", prefix, path));
        int remaining = diff.deleted().size() - LOGGED_PATH_LIMIT;
        if (remaining > 0) {
            getLogger().lifecycle("{} ... and {} more files", prefix, remaining);
        }
    }

    private Path packageRoot(PackageResult packageResult) {
        Path outputDirectory = getPackageOutputDirectory().get().getAsFile().toPath().toAbsolutePath().normalize();
        Path outputRoot = packageResult.outputRoot().toAbsolutePath().normalize();
        if (!outputRoot.equals(outputDirectory)) {
            throw new GradleException("Task '" + getPath() + "' expected mutable package output at " + outputDirectory
                    + " but the package result points at " + outputRoot + ".");
        }
        return outputRoot;
    }

    private Map<String, String> remoteConfig() {
        EffectiveConfigPlan plan = new EffectiveConfigPlanner().plan(new EffectiveConfigRequest(
                Map.of(),
                null,
                null,
                java.util.Set.of(),
                getQuarkusBuildProperties().get(),
                Map.of(),
                invocationOptions(),
                Map.of(),
                gradleProperties(),
                environmentVariables(),
                systemProperties(),
                Map.of(),
                "dev"));
        return plan.fullValues();
    }

    private Map<String, String> invocationOptions() {
        Map<String, String> options = new LinkedHashMap<>();
        if (liveReloadUrl != null && !liveReloadUrl.isBlank()) {
            options.put("quarkus.live-reload.url", liveReloadUrl);
        }
        if (liveReloadPassword != null && !liveReloadPassword.isBlank()) {
            options.put("quarkus.live-reload.password", liveReloadPassword);
        }
        return options;
    }

    private URI remoteUrl(Map<String, String> remoteConfig) {
        String value = remoteConfig.get("quarkus.live-reload.url");
        if (value == null || value.isBlank()) {
            throw new GradleException("Task '" + getPath()
                    + "' requires quarkus.live-reload.url or --live-reload-url to connect to remote dev.");
        }
        return URI.create(value);
    }

    private void validateContinuousBuild() {
        if (!getContinuousBuild().get()) {
            throw new GradleException("Task '" + getPath() + "' requires Gradle continuous build. "
                    + "Run it with --continuous.");
        }
    }

    private void writeReceipt(long sequence, String outcome, int changed, int deleted, boolean started, Path packageRoot)
            throws IOException {
        Path receipt = getReceiptFile().get().getAsFile().toPath();
        if (receipt.getParent() != null) {
            Files.createDirectories(receipt.getParent());
        }
        Files.writeString(receipt, """
                sequence=%d
                outcome=%s
                changed=%d
                deleted=%d
                started=%s
                package-root=%s
                """.formatted(sequence, outcome, changed, deleted, started, packageRoot), StandardCharsets.UTF_8);
    }
}
