package io.quarkus.banner.deployment;

import java.util.function.BooleanSupplier;

public class IsBanner implements BooleanSupplier {

    private final BannerConfig bannerConfig;

    public IsBanner(BannerConfig bannerConfig) {
        this.bannerConfig = bannerConfig;
    }

    @Override
    public boolean getAsBoolean() {
        return bannerConfig.enabled;
    }
}
