package io.quarkus.deployment.pkg.steps;

import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;

import io.quarkus.deployment.configuration.NativeConfigUtils;
import io.quarkus.deployment.pkg.NativeConfig;

public enum NativeImageFutureDefault {
    RUN_TIME_INITIALIZE_FILE_SYSTEM_PROVIDERS,
    RUN_TIME_INITIALIZE_SECURITY_PROVIDERS;

    private static final String FUTURE_DEFAULTS_MARKER = "--future-defaults=";

    public boolean isEnabled(NativeConfig nativeConfig) {
        return isFutureDefault(this, nativeConfig);
    }

    private static boolean isFutureDefault(NativeImageFutureDefault futureDefault, NativeConfig nativeConfig) {
        final List<String> additionalBuildArgs = NativeConfigUtils.getNativeAdditionalBuildArgs(nativeConfig);
        for (String buildArg : additionalBuildArgs) {
            String trimmedBuildArg = buildArg.trim();
            if (trimmedBuildArg.contains(FUTURE_DEFAULTS_MARKER)) {
                int index = trimmedBuildArg.indexOf('=');
                String[] futureDefaultStringArgs = trimmedBuildArg.substring(index + 1).split(",");
                for (String futureDefaultString : futureDefaultStringArgs) {
                    if ("all".equals(futureDefaultString)) {
                        return true;
                    }

                    if ("run-time-initialize-jdk".equals(futureDefaultString)) {
                        switch (futureDefault) {
                            case RUN_TIME_INITIALIZE_SECURITY_PROVIDERS:
                            case RUN_TIME_INITIALIZE_FILE_SYSTEM_PROVIDERS:
                                return true;
                        }
                    }

                    final NativeImageFutureDefault futureDefaultArg = NativeImageFutureDefault
                            .valueOf(futureDefaultString.toUpperCase(Locale.ROOT).replace('-', '_'));
                    if (futureDefaultArg == futureDefault) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static abstract class AbstractNativeImageFutureDefaultBooleanSupplier implements BooleanSupplier {
        protected final NativeConfig nativeConfig;

        public AbstractNativeImageFutureDefaultBooleanSupplier(final NativeConfig nativeConfig) {
            this.nativeConfig = nativeConfig;
        }
    }

    public static final class RunTimeInitializeFileSystemProvider extends AbstractNativeImageFutureDefaultBooleanSupplier {
        public RunTimeInitializeFileSystemProvider(NativeConfig nativeConfig) {
            super(nativeConfig);
        }

        @Override
        public boolean getAsBoolean() {
            return isFutureDefault(NativeImageFutureDefault.RUN_TIME_INITIALIZE_FILE_SYSTEM_PROVIDERS, nativeConfig);
        }
    }

    public static final class RunTimeInitializeSecurityProvider extends AbstractNativeImageFutureDefaultBooleanSupplier {
        public RunTimeInitializeSecurityProvider(NativeConfig nativeConfig) {
            super(nativeConfig);
        }

        @Override
        public boolean getAsBoolean() {
            return isFutureDefault(NativeImageFutureDefault.RUN_TIME_INITIALIZE_SECURITY_PROVIDERS, nativeConfig);
        }
    }
}
