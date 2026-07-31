package io.quarkus.deployment.builditem;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.app.DependencyInfoProvider;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.PlatformImports;
import io.quarkus.builder.Version;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.BootstrapConfig;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.smallrye.common.version.VersionScheme;

/**
 * A build item used to provide a application dependency model.
 *
 * @see ApplicationModel
 */
public final class AppModelProviderBuildItem extends SimpleBuildItem {

    private static final Logger log = Logger.getLogger(AppModelProviderBuildItem.class);

    private static final Pattern REQUIRES_QUARKUS_CORE_PATTERN = Pattern
            .compile("requires-quarkus-core:\\s*\"?([^\"\\s]+)\"?");

    /**
     * The version used by the main branch of Quarkus and of extensions built against it. It never satisfies
     * upper-bounded {@code requires-quarkus-core} ranges, so the compatibility check is skipped for it.
     */
    private static final String MAIN_BRANCH_VERSION = "999-SNAPSHOT";

    private final ApplicationModel appModel;

    private final Supplier<DependencyInfoProvider> depInfoProvider;

    public AppModelProviderBuildItem(ApplicationModel appModel) {
        this(appModel, null);
    }

    public AppModelProviderBuildItem(ApplicationModel appModel, Supplier<DependencyInfoProvider> depInfoProvider) {
        this.appModel = Objects.requireNonNull(appModel);
        this.depInfoProvider = depInfoProvider;
    }

    /**
     * Validates the platform imports in the application model against the provided bootstrap configuration.
     * The behavior in case of misalignment depends on the provided {@link BootstrapConfig#misalignedPlatformImports()}:
     * <ul>
     * <li><b>ERROR</b>: Throws a {@link RuntimeException}.</li>
     * <li><b>WARN</b>: Logs a warning.</li>
     * <li><b>IGNORE</b>: Skips validation entirely.</li>
     * </ul>
     * <p>
     * Additionally validates that the Quarkus core version used by the application satisfies the
     * {@code requires-quarkus-core} version range declared by each extension present in the application model.
     * The behavior in case of an incompatible extension depends on the provided
     * {@link BootstrapConfig#incompatibleExtensions()} in the same way as described above.
     *
     * @param config the bootstrap configuration
     * @return the validated application model
     * @throws RuntimeException if platform imports are misaligned and the configuration is set to {@code ERROR}
     *         or if the configuration is unrecognized.
     */
    public ApplicationModel validateAndGet(BootstrapConfig config) {
        final PlatformImports platforms = appModel.getPlatforms();
        if (platforms != null && !BootstrapConfig.MisalignedPlatformImports.IGNORE.equals(config.misalignedPlatformImports())
                && !platforms.isAligned()) {
            switch (config.misalignedPlatformImports()) {
                case ERROR:
                    throw new RuntimeException(platforms.getMisalignmentReport());
                case WARN:
                    log.warn(platforms.getMisalignmentReport());
                    break;
                default:
                    throw new RuntimeException("Unrecognized option for quarkus.bootstrap.misaligned-platform-imports: "
                            + config.misalignedPlatformImports());
            }
        }
        validateExtensionCompatibility(config.incompatibleExtensions(), Version.getVersion());
        return appModel;
    }

    void validateExtensionCompatibility(BootstrapConfig.IncompatibleExtensions action, String quarkusCoreVersion) {
        if (BootstrapConfig.IncompatibleExtensions.IGNORE.equals(action)) {
            return;
        }
        if (MAIN_BRANCH_VERSION.equals(quarkusCoreVersion)) {
            log.debugf("Quarkus core version is %s; skipping the extension compatibility check", quarkusCoreVersion);
            return;
        }
        final StringBuilder incompatible = new StringBuilder();
        for (ResolvedDependency dep : appModel.getDependencies()) {
            if (!dep.isRuntimeExtensionArtifact()) {
                continue;
            }
            final String requiresQuarkusCore = readRequiresQuarkusCore(dep);
            if (requiresQuarkusCore == null
                    || (!requiresQuarkusCore.startsWith("[") && !requiresQuarkusCore.startsWith("("))) {
                /* No explicit version range declared by the extension */
                continue;
            }
            try {
                if (VersionScheme.MAVEN.fromRangeString(requiresQuarkusCore).test(quarkusCoreVersion)) {
                    continue;
                }
            } catch (RuntimeException e) {
                log.debugf(e, "Could not evaluate requires-quarkus-core range %s of %s;"
                        + " skipping the compatibility check for this extension", requiresQuarkusCore,
                        dep.toCompactCoords());
                continue;
            }
            incompatible.append(System.lineSeparator())
                    .append("- ").append(dep.toCompactCoords())
                    .append(" (requires Quarkus core ").append(requiresQuarkusCore).append(")");
        }
        if (incompatible.length() == 0) {
            return;
        }
        final String report = "The following extensions declare via the requires-quarkus-core metadata that they are"
                + " incompatible with the Quarkus core version used by the application (" + quarkusCoreVersion + "):"
                + incompatible
                + System.lineSeparator()
                + "This may lead to obscure build time or runtime failures."
                + " Please align the versions in your dependency management, e.g. by importing a matching Quarkus"
                + " platform BOM stream. This check can be configured via"
                + " quarkus.bootstrap.incompatible-extensions (error, warn, ignore).";
        switch (action) {
            case ERROR:
                throw new RuntimeException(report);
            case WARN:
                log.warn(report);
                break;
            default:
                throw new RuntimeException("Unrecognized option for quarkus.bootstrap.incompatible-extensions: "
                        + action);
        }
    }

    private static String readRequiresQuarkusCore(ResolvedDependency dep) {
        final String descriptor = dep.getContentTree().apply(BootstrapConstants.EXTENSION_METADATA_PATH, visit -> {
            if (visit == null) {
                return null;
            }
            try {
                return Files.readString(visit.getPath());
            } catch (IOException e) {
                throw new UncheckedIOException(
                        "Could not read " + BootstrapConstants.EXTENSION_METADATA_PATH + " from " + visit.getUrl(), e);
            }
        });
        return descriptor == null ? null : parseRequiresQuarkusCore(descriptor);
    }

    /**
     * @param extensionYaml the content of a {@code META-INF/quarkus-extension.yaml} file
     * @return the value of the {@code requires-quarkus-core} attribute or {@code null} if it could not be found
     */
    static String parseRequiresQuarkusCore(String extensionYaml) {
        final Matcher m = REQUIRES_QUARKUS_CORE_PATTERN.matcher(extensionYaml);
        return m.find() ? m.group(1) : null;
    }

    public Supplier<DependencyInfoProvider> getDependencyInfoProvider() {
        return depInfoProvider;
    }
}
