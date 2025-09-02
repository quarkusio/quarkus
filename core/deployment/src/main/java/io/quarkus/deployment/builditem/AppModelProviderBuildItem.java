package io.quarkus.deployment.builditem;

import java.util.Objects;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.app.DependencyInfoProvider;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.PlatformImports;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.BootstrapConfig;

/**
 * A build item used to provide a application dependency model.
 *
 * @see ApplicationModel
 */
public final class AppModelProviderBuildItem extends SimpleBuildItem {

    private static final Logger log = Logger.getLogger(AppModelProviderBuildItem.class);

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
        return appModel;
    }

    public Supplier<DependencyInfoProvider> getDependencyInfoProvider() {
        return depInfoProvider;
    }
}
