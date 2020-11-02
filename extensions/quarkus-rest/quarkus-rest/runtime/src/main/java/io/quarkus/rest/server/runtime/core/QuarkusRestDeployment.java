package io.quarkus.rest.server.runtime.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import javax.ws.rs.core.Application;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;

import io.quarkus.rest.common.runtime.jaxrs.QuarkusRestConfiguration;
import io.quarkus.rest.common.runtime.model.ResourceParamConverterProvider;
import io.quarkus.rest.common.runtime.util.Types;
import io.quarkus.rest.server.runtime.core.parameters.converters.ParameterConverter;
import io.quarkus.rest.server.runtime.core.parameters.converters.RuntimeParameterConverter;
import io.quarkus.rest.server.runtime.core.serialization.EntityWriter;
import io.quarkus.rest.server.runtime.handlers.ServerRestHandler;
import io.quarkus.rest.spi.BeanFactory.BeanInstance;

public class QuarkusRestDeployment {
    private final ExceptionMapping exceptionMapping;
    private final ContextResolvers contextResolvers;
    private final ServerSerialisers serialisers;
    private final ServerRestHandler[] abortHandlerChain;
    private final EntityWriter dynamicEntityWriter;
    private final String prefix;
    private final ParamConverterProviders paramConverterProviders;
    private final QuarkusRestConfiguration configuration;
    private final Supplier<Application> applicationSupplier;

    public QuarkusRestDeployment(ExceptionMapping exceptionMapping, ContextResolvers contextResolvers,
            ServerSerialisers serialisers,
            ServerRestHandler[] abortHandlerChain,
            EntityWriter dynamicEntityWriter, String prefix, ParamConverterProviders paramConverterProviders,
            QuarkusRestConfiguration configuration, Supplier<Application> applicationSupplier) {
        this.exceptionMapping = exceptionMapping;
        this.contextResolvers = contextResolvers;
        this.serialisers = serialisers;
        this.abortHandlerChain = abortHandlerChain;
        this.dynamicEntityWriter = dynamicEntityWriter;
        this.prefix = prefix;
        this.paramConverterProviders = paramConverterProviders;
        this.configuration = configuration;
        this.applicationSupplier = applicationSupplier;
    }

    public Supplier<Application> getApplicationSupplier() {
        return applicationSupplier;
    }

    public QuarkusRestConfiguration getConfiguration() {
        return configuration;
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
}
