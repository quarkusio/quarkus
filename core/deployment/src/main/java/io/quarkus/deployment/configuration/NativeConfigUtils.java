package io.quarkus.deployment.configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.quarkus.deployment.pkg.NativeConfig;

public final class NativeConfigUtils {
    public static List<String> getNativeAdditionalBuildArgs(NativeConfig nativeConfig) {
        List<String> additionalBuildArgs = new ArrayList<>();
        nativeConfig.additionalBuildArgs().map(additionalBuildArgs::addAll);
        nativeConfig.additionalBuildArgsAppend().map(additionalBuildArgs::addAll);

        final String nativeImageOptions = System.getenv().get("NATIVE_IMAGE_OPTIONS");
        if (nativeImageOptions != null) {
            // Native image options are space separated,
            // it follows same format as JAVA_TOOL_OPTIONS.
            final String[] options = nativeImageOptions.split("\\s+");
            additionalBuildArgs.addAll(Arrays.asList(options));
        }

        return additionalBuildArgs;
    }

    private NativeConfigUtils() {
    }
}
