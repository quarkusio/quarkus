package org.jboss.resteasy.reactive.server.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;

import org.jboss.resteasy.reactive.common.ResteasyReactiveConfig;
import org.jboss.resteasy.reactive.common.jaxrs.ConfigurationImpl;
import org.jboss.resteasy.reactive.common.model.ResourceParamConverterProvider;
import org.jboss.resteasy.reactive.common.util.types.Types;
import org.jboss.resteasy.reactive.server.core.parameters.converters.ParameterConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.RuntimeParameterConverter;
import org.jboss.resteasy.reactive.server.core.serialization.EntityWriter;
import org.jboss.resteasy.reactive.server.handlers.RestInitialHandler;
import org.jboss.resteasy.reactive.server.mapping.RequestMapper;
import org.jboss.resteasy.reactive.server.model.ContextResolvers;
import org.jboss.resteasy.reactive.server.model.ParamConverterProviders;
import org.jboss.resteasy.reactive.server.spi.GenericRuntimeConfigurableServerRestHandler;
import org.jboss.resteasy.reactive.server.spi.RuntimeConfiguration;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;
import org.jboss.resteasy.reactive.spi.BeanFactory.BeanInstance;
import org.jboss.resteasy.reactive.spi.ThreadSetupAction;

public class Deployment {
    private final ExceptionMapping exceptionMapping;
    private final ContextResolvers contextResolvers;
    private final ServerSerialisers serialisers;
    private final ServerRestHandler[] abortHandlerChain;
    private final EntityWriter dynamicEntityWriter;
    private final String prefix;
    private final ParamConverterProviders paramConverterProviders;
    private final ConfigurationImpl configuration;
    private final Supplier<Application> applicationSupplier;
    private final ThreadSetupAction threadSetupAction;
    private final RequestContextFactory requestContextFactory;
    private final List<ServerRestHandler> preMatchHandlers;
    private final ArrayList<RequestMapper.RequestPath<RestInitialHandler.InitialMatch>> classMappers;
    private final List<GenericRuntimeConfigurableServerRestHandler<?>> runtimeConfigurableServerRestHandlers;
    private final RuntimeExceptionMapper exceptionMapper;
    private final boolean resumeOn404;
    private final ResteasyReactiveConfig resteasyReactiveConfig;
    private final Map<String, List<String>> disabledEndpoints;
    //this is not final, as it is set after startup
    private RuntimeConfiguration runtimeConfiguration;

    public Deployment(ExceptionMapping exceptionMapping, ContextResolvers contextResolvers,
            ServerSerialisers serialisers,
            ServerRestHandler[] abortHandlerChain,
            EntityWriter dynamicEntityWriter, String prefix, ParamConverterProviders paramConverterProviders,
            ConfigurationImpl configuration, Supplier<Application> applicationSupplier,
            ThreadSetupAction threadSetupAction, RequestContextFactory requestContextFactory,
            List<ServerRestHandler> preMatchHandlers,
            ArrayList<RequestMapper.RequestPath<RestInitialHandler.InitialMatch>> classMappers,
            List<GenericRuntimeConfigurableServerRestHandler<?>> runtimeConfigurableServerRestHandlers,
            RuntimeExceptionMapper exceptionMapper,
            boolean resumeOn404,
            ResteasyReactiveConfig resteasyReactiveConfig,
            Map<String, List<String>> disabledEndpoints) {
        this.exceptionMapping = exceptionMapping;
        this.contextResolvers = contextResolvers;
        this.serialisers = serialisers;
        this.abortHandlerChain = abortHandlerChain;
        this.dynamicEntityWriter = dynamicEntityWriter;
        this.prefix = prefix;
        this.paramConverterProviders = paramConverterProviders;
        this.configuration = configuration;
        this.applicationSupplier = applicationSupplier;
        this.threadSetupAction = threadSetupAction;
        this.requestContextFactory = requestContextFactory;
        this.preMatchHandlers = preMatchHandlers;
        this.classMappers = classMappers;
        this.runtimeConfigurableServerRestHandlers = runtimeConfigurableServerRestHandlers;
        this.exceptionMapper = exceptionMapper;
        this.resumeOn404 = resumeOn404;
        this.resteasyReactiveConfig = resteasyReactiveConfig;
        this.disabledEndpoints = disabledEndpoints;
    }

    public RuntimeExceptionMapper getExceptionMapper() {
        return exceptionMapper;
    }

    public Supplier<Application> getApplicationSupplier() {
        return applicationSupplier;
    }

    public ConfigurationImpl getConfiguration() {
        return configuration;
    }

    public ResteasyReactiveConfig getResteasyReactiveConfig() {
        return resteasyReactiveConfig;
    }

    public ExceptionMapping getExceptionMapping() {
        return exceptionMapping;
    }

    public ContextResolvers getContextResolvers() {
        return contextResolvers;
    }

    public ServerSerialisers getSerialisers() {
        return serialisers;
    }

    public ServerRestHandler[] getAbortHandlerChain() {
        return abortHandlerChain;
    }

    public EntityWriter getDynamicEntityWriter() {
        return dynamicEntityWriter;
    }

    public boolean isResumeOn404() {
        return resumeOn404;
    }

    /**
     * Application path prefix. Must start with "/" and not end with a "/". Cannot be null.
     *
     * @return the application path prefix, or an empty string.
     */
    public String getPrefix() {
        return prefix;
    }

    public ParamConverterProviders getParamConverterProviders() {
        return paramConverterProviders;
    }

    public List<ServerRestHandler> getPreMatchHandlers() {
        return preMatchHandlers;
    }

    public ArrayList<RequestMapper.RequestPath<RestInitialHandler.InitialMatch>> getClassMappers() {
        return classMappers;
    }

    public ParameterConverter getRuntimeParamConverter(Class<?> fieldOwnerClass, String fieldName, boolean single) {
        List<ResourceParamConverterProvider> providers = getParamConverterProviders().getParamConverterProviders();
        if (providers.size() > 0) {
            Field field;
            try {
                field = fieldOwnerClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException | SecurityException e) {
                throw new RuntimeException(e);
            }
            Class<?> klass;
            Type genericType;
            if (single) {
                klass = field.getType();
                genericType = field.getGenericType();
            } else {
                genericType = field.getGenericType();
                if (genericType instanceof ParameterizedType) {
                    Type[] args = Types.findInterfaceParameterizedTypes(field.getType(), (ParameterizedType) genericType,
                            Collection.class);
                    if (args != null && args.length == 1) {
                        genericType = args[0];
                        klass = Types.getRawType(genericType);
                    } else {
                        throw new RuntimeException("Failed to find Collection supertype of " + field);
                    }
                } else {
                    throw new RuntimeException("Failed to find Collection supertype of " + field);
                }
            }
            Annotation[] annotations = field.getAnnotations();
            for (ResourceParamConverterProvider converterProvider : providers) {
                BeanInstance<ParamConverterProvider> instance = converterProvider.getFactory().createInstance();
                ParamConverter<?> converter = instance.getInstance().getConverter(klass, genericType, annotations);
                if (converter != null)
                    return new RuntimeParameterConverter(converter);
            }
        }
        return null;
    }

    public ThreadSetupAction getThreadSetupAction() {
        return threadSetupAction;
    }

    public RequestContextFactory getRequestContextFactory() {
        return requestContextFactory;
    }

    public List<GenericRuntimeConfigurableServerRestHandler<?>> getRuntimeConfigurableServerRestHandlers() {
        return runtimeConfigurableServerRestHandlers;
    }

    public RuntimeConfiguration getRuntimeConfiguration() {
        return runtimeConfiguration;
    }

    public Deployment setRuntimeConfiguration(RuntimeConfiguration runtimeConfiguration) {
        if (this.runtimeConfiguration != null) {
            throw new IllegalStateException("runtime config has already been set");
        }
        this.runtimeConfiguration = runtimeConfiguration;
        return this;
    }

    public Map<String, List<String>> getDisabledEndpoints() {
        return disabledEndpoints;
    }
}
