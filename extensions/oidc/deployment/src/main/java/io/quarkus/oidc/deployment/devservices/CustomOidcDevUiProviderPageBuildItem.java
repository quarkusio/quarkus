package io.quarkus.oidc.deployment.devservices;

import java.util.Objects;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.devui.spi.page.PageBuilder;

/**
 * Replace default OIDC provider DEV UI page with one specific to your provider.
 */
public final class CustomOidcDevUiProviderPageBuildItem extends SimpleBuildItem {

    private final PageBuilder<?> oidcProviderPage;

    public CustomOidcDevUiProviderPageBuildItem(PageBuilder<?> oidcProviderPage) {
        this.oidcProviderPage = Objects.requireNonNull(oidcProviderPage);
    }

    public PageBuilder<?> getOidcProviderPage() {
        return oidcProviderPage;
    }
}
