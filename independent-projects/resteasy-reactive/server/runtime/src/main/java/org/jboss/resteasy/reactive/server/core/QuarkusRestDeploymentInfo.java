package org.jboss.resteasy.reactive.server.core;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.ws.rs.core.Application;
import org.jboss.resteasy.reactive.common.ResteasyReactiveConfig;
import org.jboss.resteasy.reactive.common.model.ResourceClass;
import org.jboss.resteasy.reactive.common.model.ResourceInterceptors;
import org.jboss.resteasy.reactive.spi.BeanFactory;

public class QuarkusRestDeploymentInfo {

    private ResourceInterceptors interceptors;
    private ExceptionMapping exceptionMapping;
    private ContextResolvers ctxResolvers;
    private Features features;
    private DynamicFeatures dynamicFeatures;
    private ServerSerialisers serialisers;
    private List<ResourceClass> resourceClasses;
    private List<ResourceClass> locatableResourceClasses;
    private ParamConverterProviders paramConverterProviders;
    private Supplier<Application> applicationSupplier;
    private Function<Class<?>, BeanFactory<?>> factoryCreator;
    private ResteasyReactiveConfig config;

    public ResourceInterceptors getInterceptors() {
        return interceptors;
    }

    public QuarkusRestDeploymentInfo setInterceptors(ResourceInterceptors interceptors) {
        this.interceptors = interceptors;
        return this;
    }

    public ExceptionMapping getExceptionMapping() {
        return exceptionMapping;
    }

    public QuarkusRestDeploymentInfo setExceptionMapping(ExceptionMapping exceptionMapping) {
        this.exceptionMapping = exceptionMapping;
        return this;
    }

    public ContextResolvers getCtxResolvers() {
        return ctxResolvers;
    }

    public QuarkusRestDeploymentInfo setCtxResolvers(ContextResolvers ctxResolvers) {
        this.ctxResolvers = ctxResolvers;
        return this;
    }

    public Features getFeatures() {
        return features;
    }

    public QuarkusRestDeploymentInfo setFeatures(Features features) {
        this.features = features;
        return this;
    }

    public DynamicFeatures getDynamicFeatures() {
        return dynamicFeatures;
    }

    public QuarkusRestDeploymentInfo setDynamicFeatures(DynamicFeatures dynamicFeatures) {
        this.dynamicFeatures = dynamicFeatures;
        return this;
    }

    public ServerSerialisers getSerialisers() {
        return serialisers;
    }

    public QuarkusRestDeploymentInfo setSerialisers(ServerSerialisers serialisers) {
        this.serialisers = serialisers;
        return this;
    }

    public List<ResourceClass> getResourceClasses() {
        return resourceClasses;
    }

    public QuarkusRestDeploymentInfo setResourceClasses(List<ResourceClass> resourceClasses) {
        this.resourceClasses = resourceClasses;
        return this;
    }

    public List<ResourceClass> getLocatableResourceClasses() {
        return locatableResourceClasses;
    }

    public QuarkusRestDeploymentInfo setLocatableResourceClasses(List<ResourceClass> locatableResourceClasses) {
        this.locatableResourceClasses = locatableResourceClasses;
        return this;
    }

    public ParamConverterProviders getParamConverterProviders() {
        return paramConverterProviders;
    }

    public QuarkusRestDeploymentInfo setParamConverterProviders(ParamConverterProviders paramConverterProviders) {
        this.paramConverterProviders = paramConverterProviders;
        return this;
    }

    public QuarkusRestDeploymentInfo setFactoryCreator(Function<Class<?>, BeanFactory<?>> factoryCreator) {
        this.factoryCreator = factoryCreator;
        return this;
    }

    public Function<Class<?>, BeanFactory<?>> getFactoryCreator() {
        return factoryCreator;
    }

    public Supplier<Application> getApplicationSupplier() {
        return applicationSupplier;
    }

    public QuarkusRestDeploymentInfo setApplicationSupplier(Supplier<Application> applicationSupplier) {
        this.applicationSupplier = applicationSupplier;
        return this;
    }

    public ResteasyReactiveConfig getConfig() {
        return config;
    }

    public QuarkusRestDeploymentInfo setConfig(ResteasyReactiveConfig config) {
        this.config = config;
        return this;
    }
}
