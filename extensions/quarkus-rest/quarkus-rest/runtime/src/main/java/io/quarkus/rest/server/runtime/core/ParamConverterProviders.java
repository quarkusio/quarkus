package io.quarkus.rest.server.runtime.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.resteasy.reactive.common.runtime.model.ResourceParamConverterProvider;

public class ParamConverterProviders {

    private final List<ResourceParamConverterProvider> paramConverterProviders = new ArrayList<>();

    public void addParamConverterProviders(ResourceParamConverterProvider resourceFeature) {
        paramConverterProviders.add(resourceFeature);
    }

    public List<ResourceParamConverterProvider> getParamConverterProviders() {
        return paramConverterProviders;
    }

    public void sort() {
        Collections.sort(paramConverterProviders);
    }
}
