package io.quarkus.vertx.core.runtime.config;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.vertx")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface VertxBuildTimeConfig {

    /**
     * Whether and how strictly native transport should be used.
     * <p>
     * Supported values:
     * <ul>
     * <li>{@code disabled} - Do not use native transport (default). Vert.x always uses Java NIO.</li>
     * <li>{@code if-available} - Use native transport if available, fall back to Java NIO otherwise.</li>
     * <li>{@code required} - Require native transport. If the transport dependency is missing, the build fails.
     * If the native library cannot load at runtime, the application fails to start.</li>
     * </ul>
     */
    @WithDefault("disabled")
    NativeTransportMode nativeTransport();

    /**
     * The type of native transport to use.
     * <p>
     * When set to a value other than {@code auto}, the application will attempt to use the specified
     * native transport. If the transport is not available at runtime, the behavior depends on
     * {@link #nativeTransport()}.
     * <p>
     * Setting this to a specific transport type implicitly enables native transport
     * (i.e., {@link #nativeTransport()} is treated as at least {@code if-available}).
     * <p>
     * Supported values:
     * <ul>
     * <li>{@code auto} - Let Vert.x pick the best available transport (default)</li>
     * <li>{@code epoll} - Use Linux epoll transport</li>
     * <li>{@code kqueue} - Use macOS kqueue transport</li>
     * <li>{@code io-uring} - Use Linux io_uring transport</li>
     * </ul>
     * <p>
     * Note that, if not set to {@code auto}, Quarkus checks at build time that the requested transport type is
     * available in the classpath. At runtime, it checks whether the transport can be used.
     */
    @WithDefault("auto")
    NativeTransportType nativeTransportType();
}
