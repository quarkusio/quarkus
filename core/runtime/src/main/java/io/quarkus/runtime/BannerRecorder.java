package io.quarkus.runtime;

import java.util.Optional;
import java.util.function.Supplier;

import org.jboss.logmanager.handlers.ConsoleHandler;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class BannerRecorder {
    final RuntimeValue<BannerRuntimeConfig> bannerRuntimeConfig;

    public BannerRecorder(RuntimeValue<BannerRuntimeConfig> bannerRuntimeConfig) {
        this.bannerRuntimeConfig = bannerRuntimeConfig;
    }

    public RuntimeValue<Optional<Supplier<String>>> provideBannerSupplier(String bannerText, String graphicalBannerText) {
        BannerRuntimeConfig bc = bannerRuntimeConfig.getValue();
        if (bc.enabled) {
            if (bc.image.enabled && !graphicalBannerText.isEmpty()
                    && ConsoleHandler.isGraphicsSupportPassivelyDetected()) {
                return new RuntimeValue<>(Optional.of(new ConstantSupplier(graphicalBannerText)));
            } else {
                if (bc.noWrap) {
                    bannerText = "\033[7l" + bannerText + "\033[7h";
                }
                return new RuntimeValue<>(Optional.of(new ConstantSupplier(bannerText)));
            }
        } else {
            return new RuntimeValue<>(Optional.empty());
        }
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
