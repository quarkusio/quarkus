package org.jboss.resteasy.reactive.server.model;

import jakarta.ws.rs.ext.ParamConverterProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import org.jboss.resteasy.reactive.common.model.ResourceParamConverterProvider;
import org.jboss.resteasy.reactive.spi.BeanFactory;

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

    public void initializeDefaultFactories(Function<String, BeanFactory<?>> factoryCreator) {
        for (ResourceParamConverterProvider i : paramConverterProviders) {
            i.setFactory((BeanFactory<ParamConverterProvider>) factoryCreator.apply(i.getClassName()));
        }
    }
}
