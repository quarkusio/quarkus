package org.jboss.resteasy.reactive.server.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.ws.rs.core.Application;
import org.jboss.resteasy.reactive.common.ResteasyReactiveConfig;
import org.jboss.resteasy.reactive.common.model.ResourceClass;
import org.jboss.resteasy.reactive.common.model.ResourceInterceptors;
import org.jboss.resteasy.reactive.server.model.ContextResolvers;
import org.jboss.resteasy.reactive.server.model.DynamicFeatures;
import org.jboss.resteasy.reactive.server.model.Features;
import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.model.ParamConverterProviders;
import org.jboss.resteasy.reactive.spi.BeanFactory;

public class DeploymentInfo {

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
    private Function<Object, Object> clientProxyUnwrapper;
    private String applicationPath;
    private List<HandlerChainCustomizer> globalHandlerCustomers = new ArrayList<>();
    private boolean developmentMode;

    public ResourceInterceptors getInterceptors() {
        return interceptors;
    }

    public DeploymentInfo setInterceptors(ResourceInterceptors interceptors) {
        this.interceptors = interceptors;
        return this;
    }

    public ExceptionMapping getExceptionMapping() {
        return exceptionMapping;
    }

    public DeploymentInfo setExceptionMapping(ExceptionMapping exceptionMapping) {
        this.exceptionMapping = exceptionMapping;
        return this;
    }

    public ContextResolvers getCtxResolvers() {
        return ctxResolvers;
    }

    public DeploymentInfo setCtxResolvers(ContextResolvers ctxResolvers) {
        this.ctxResolvers = ctxResolvers;
        return this;
    }

    public Features getFeatures() {
        return features;
    }

    public DeploymentInfo setFeatures(Features features) {
        this.features = features;
        return this;
    }

    public DynamicFeatures getDynamicFeatures() {
        return dynamicFeatures;
    }

    public DeploymentInfo setDynamicFeatures(DynamicFeatures dynamicFeatures) {
        this.dynamicFeatures = dynamicFeatures;
        return this;
    }

    public ServerSerialisers getSerialisers() {
        return serialisers;
    }

    public DeploymentInfo setSerialisers(ServerSerialisers serialisers) {
        this.serialisers = serialisers;
        return this;
    }

    public List<ResourceClass> getResourceClasses() {
        return resourceClasses;
    }

    public DeploymentInfo setResourceClasses(List<ResourceClass> resourceClasses) {
        this.resourceClasses = resourceClasses;
        return this;
    }

    public List<ResourceClass> getLocatableResourceClasses() {
        return locatableResourceClasses;
    }

    public DeploymentInfo setLocatableResourceClasses(List<ResourceClass> locatableResourceClasses) {
        this.locatableResourceClasses = locatableResourceClasses;
        return this;
    }

    public ParamConverterProviders getParamConverterProviders() {
        return paramConverterProviders;
    }

    public DeploymentInfo setParamConverterProviders(ParamConverterProviders paramConverterProviders) {
        this.paramConverterProviders = paramConverterProviders;
        return this;
    }

    public DeploymentInfo setFactoryCreator(Function<Class<?>, BeanFactory<?>> factoryCreator) {
        this.factoryCreator = factoryCreator;
        return this;
    }

    public Function<Class<?>, BeanFactory<?>> getFactoryCreator() {
        return factoryCreator;
    }

    public Supplier<Application> getApplicationSupplier() {
        return applicationSupplier;
    }

    public DeploymentInfo setApplicationSupplier(Supplier<Application> applicationSupplier) {
        this.applicationSupplier = applicationSupplier;
        return this;
    }

    public Function<Object, Object> getClientProxyUnwrapper() {
        return clientProxyUnwrapper;
    }

    public DeploymentInfo setClientProxyUnwrapper(Function<Object, Object> clientProxyUnwrapper) {
        this.clientProxyUnwrapper = clientProxyUnwrapper;
        return this;
    }

    public ResteasyReactiveConfig getConfig() {
        return config;
    }

    public DeploymentInfo setConfig(ResteasyReactiveConfig config) {
        this.config = config;
        return this;
    }

    public String getApplicationPath() {
        return applicationPath;
    }

    public DeploymentInfo setApplicationPath(String applicationPath) {
        this.applicationPath = applicationPath;
        return this;
    }

    public List<HandlerChainCustomizer> getGlobalHandlerCustomizers() {
        return globalHandlerCustomers;
    }

    public DeploymentInfo setGlobalHandlerCustomers(List<HandlerChainCustomizer> globalHandlerCustomers) {
        this.globalHandlerCustomers = globalHandlerCustomers;
        return this;
    }

    public boolean isDevelopmentMode() {
        return developmentMode;
    }

    public DeploymentInfo setDevelopmentMode(boolean developmentMode) {
        this.developmentMode = developmentMode;
        return this;
    }
}
