package io.quarkus.vertx.core.runtime.config;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.vertx")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface VertxBuildTimeConfig {

    /**
     * Enable or disable native transport.
     */
    @WithDefault("false")
    boolean preferNativeTransport();

    /**
     * The type of native transport to use.
     * <p>
     * When set to a value other than {@code auto}, the application will attempt to use the specified
     * native transport. If the transport is not available at runtime, the behavior depends on
     * {@link #nativeTransportRequired()}.
     * <p>
     * Setting this to a specific transport type implicitly enables native transport
     * (i.e., {@link #preferNativeTransport()} is treated as {@code true}).
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

    /**
     * Whether native transport is required.
     * <p>
     * When {@code true} and native transport is requested (via {@link #preferNativeTransport()} or
     * {@link #nativeTransportType()}) but fails to load, the application will fail to start instead
     * of falling back to NIO.
     * <p>
     * When the requested transport dependency is not on the classpath, the build will fail immediately.
     */
    @WithDefault("false")
    boolean nativeTransportRequired();
}
