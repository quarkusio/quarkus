package io.quarkus.banner.runtime;

import java.util.Optional;
import java.util.function.Supplier;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class BannerRecorder {

    public RuntimeValue<Optional<Supplier<String>>> provideBannerSupplier(String bannerText) {
        Supplier<String> supplierValue = () -> bannerText;
        return new RuntimeValue<>(Optional.of(supplierValue));
    }
}
