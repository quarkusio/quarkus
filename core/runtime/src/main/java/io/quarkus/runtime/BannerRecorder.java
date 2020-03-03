package io.quarkus.runtime;

import java.util.Optional;
import java.util.function.Supplier;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class BannerRecorder {

    public RuntimeValue<Optional<Supplier<String>>> provideBannerSupplier(String bannerText,
            BannerRuntimeConfig bannerRuntimeConfig) {
        if (bannerRuntimeConfig.enabled) {
            return new RuntimeValue<>(Optional.of(new ConstantSupplier(bannerText)));
        }
        return new RuntimeValue<>(Optional.empty());
    }

    private static final class ConstantSupplier implements Supplier<String> {

        private final String value;

        public ConstantSupplier(String value) {
            this.value = value;
        }

        @Override
        public String get() {
            return value;
        }
    }
}
