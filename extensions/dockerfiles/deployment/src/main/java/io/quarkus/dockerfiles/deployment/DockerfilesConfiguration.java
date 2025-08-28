package io.quarkus.dockerfiles.deployment;

import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.dockerfiles")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface DockerfilesConfiguration {

    String DEFAULT_JVM_FROM = "registry.access.redhat.com/ubi8/openjdk-21:1.20";
    String DEFAULT_NATIVE_FROM = "registry.access.redhat.com/ubi8/ubi-minimal:8.10";

    /**
     * The from image to use for JVM based Dockerfiles
     */
    @WithDefault(DEFAULT_JVM_FROM)
    String jvmFrom();

    /**
     * The from image to use for native based Dockerfiles
     */
    @WithDefault(DEFAULT_NATIVE_FROM)
    String nativeFrom();

    default Optional<String> getConfiguredJvmFrom() {
        return ConfigProvider.getConfig().getOptionalValue("quarkus.dockerfiles.jvm-from", String.class);
    }

    default Optional<String> getConfiguredNativeFrom() {
        return ConfigProvider.getConfig().getOptionalValue("quarkus.dockerfiles.native-from", String.class);
    }
}
