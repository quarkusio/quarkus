package io.quarkus.dockerfiles.deployment;

import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot
public class DockerfilesConfiguration {

    static final String DEFAULT_JVM_FROM = "registry.access.redhat.com/ubi8/openjdk-21:1.20";
    static final String DEFAULT_NATIVE_FROM = "registry.access.redhat.com/ubi8/ubi-minimal:8.10";

    /**
     * The from image to use for JVM based Dockerfiles
     */
    @ConfigItem(defaultValue = DEFAULT_JVM_FROM)
    String jvmFrom;

    /**
     * The from image to use for native based Dockerfiles
     */
    @ConfigItem(defaultValue = DEFAULT_NATIVE_FROM)
    String nativeFrom;

    static String getDefaultJvmFrom() {
        return DEFAULT_JVM_FROM;
    }

    Optional<String> getConfiguredJvmFrom() {
        return ConfigProvider.getConfig().getOptionalValue("quarkus.dockerfiles.jvm-from", String.class);
    }

    static String getDefaultNativeFrom() {
        return DEFAULT_NATIVE_FROM;
    }

    Optional<String> getConfiguredNativeFrom() {
        return ConfigProvider.getConfig().getOptionalValue("quarkus.dockerfiles.native-from", String.class);
    }
}
