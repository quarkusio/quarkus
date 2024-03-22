package io.quarkus.runtime;

import java.util.Optional;
import java.util.function.Supplier;

import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.common.os.OS;

@Recorder
public class BannerRecorder {
    private static volatile String tipOfTheDay = null;

    private final RuntimeValue<BannerRuntimeConfig> bannerRuntimeConfig;

    public BannerRecorder(RuntimeValue<BannerRuntimeConfig> bannerRuntimeConfig) {
        this.bannerRuntimeConfig = bannerRuntimeConfig;
    }

    public RuntimeValue<Optional<Supplier<String>>> provideBannerSupplier(String bannerText) {
        if (bannerRuntimeConfig.getValue().enabled) {
            if (bannerRuntimeConfig.getValue().tips && bannerText.contains("${title}") && bannerText.contains("${message}")
                    && tipOfTheDay != null) {
                String icon = OS.WINDOWS.isCurrent() ? ">>" : "\uD83D\uDCA1";
                bannerText = bannerText.replace("${title}", icon + " TIP OF THE DAY:");
                bannerText = bannerText.replace("${message}", tipOfTheDay);
            } else {
                bannerText = bannerText.replace("${title}", "");
                bannerText = bannerText.replace("${message}", "");
            }
            return new RuntimeValue<>(Optional.of(new ConstantSupplier(bannerText)));
        }
        return new RuntimeValue<>(Optional.empty());
    }

    public void setTipOfTheDay(String tipOfTheDay) {
        BannerRecorder.tipOfTheDay = tipOfTheDay;
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
