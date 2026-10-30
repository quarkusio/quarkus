package io.quarkus.deployment.pkg;

import java.util.List;

import io.smallrye.config.ConfigInstanceBuilder;

public final class TestNativeConfig {

    private TestNativeConfig() {
    }

    public static NativeConfig of(String builderImage) {
        return of(builderImage, NativeConfig.ImagePullStrategy.ALWAYS);
    }

    public static NativeConfig of(NativeConfig.ImagePullStrategy builderImagePull) {
        return of("mandrel", builderImagePull);
    }

    public static NativeConfig of(String builderImage, NativeConfig.ImagePullStrategy builderImagePull) {
        return builder(builderImage, builderImagePull).build();
    }

    public static NativeConfig withAdditionalBuildArgs(List<String> additionalBuildArgs,
            List<String> additionalBuildArgsAppend) {
        ConfigInstanceBuilder<NativeConfig> builder = builder("dummy", NativeConfig.ImagePullStrategy.NEVER);
        if (additionalBuildArgs != null) {
            builder.withOptional(NativeConfig::additionalBuildArgs, additionalBuildArgs);
        }
        if (additionalBuildArgsAppend != null) {
            builder.withOptional(NativeConfig::additionalBuildArgsAppend, additionalBuildArgsAppend);
        }
        return builder.build();
    }

    private static ConfigInstanceBuilder<NativeConfig> builder(String builderImage,
            NativeConfig.ImagePullStrategy builderImagePull) {
        return ConfigInstanceBuilder.forInterface(NativeConfig.class)
                .with(NativeConfig::enabled, true)
                .with(NativeConfig::sourcesOnly, true)
                .with(NativeConfig::builderImage, ConfigInstanceBuilder.forInterface(NativeConfig.BuilderImageConfig.class)
                        .with(NativeConfig.BuilderImageConfig::image, builderImage)
                        .with(NativeConfig.BuilderImageConfig::pull, builderImagePull)
                        .build());
    }
}
