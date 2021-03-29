package io.quarkus.resteasy.reactive.server.deployment;

import org.jboss.resteasy.reactive.server.model.ParamConverterProviders;

import io.quarkus.builder.item.SimpleBuildItem;

final class ParamConverterProvidersBuildItem extends SimpleBuildItem {

    private final ParamConverterProviders paramConverterProviders;

    public ParamConverterProvidersBuildItem(ParamConverterProviders paramConverterProviders) {
        this.paramConverterProviders = paramConverterProviders;
    }

    public ParamConverterProviders getParamConverterProviders() {
        return paramConverterProviders;
    }
}
