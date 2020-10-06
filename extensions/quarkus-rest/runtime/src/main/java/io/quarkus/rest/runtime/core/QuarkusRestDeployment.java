package io.quarkus.rest.runtime.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;

import io.quarkus.rest.runtime.client.ClientProxies;
import io.quarkus.rest.runtime.core.parameters.converters.ParameterConverter;
import io.quarkus.rest.runtime.core.parameters.converters.RuntimePameterConverter;
import io.quarkus.rest.runtime.core.serialization.EntityWriter;
import io.quarkus.rest.runtime.handlers.RestHandler;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestConfiguration;
import io.quarkus.rest.runtime.model.ResourceParamConverterProvider;
import io.quarkus.rest.runtime.spi.BeanFactory.BeanInstance;
import io.quarkus.rest.runtime.util.Types;

public class QuarkusRestDeployment {
    private final ExceptionMapping exceptionMapping;
    private final ContextResolvers contextResolvers;
    private final Serialisers serialisers;
    private final RestHandler[] abortHandlerChain;
    private final EntityWriter dynamicEntityWriter;
    private final ClientProxies clientProxies;
    private final String prefix;
    private final GenericTypeMapping genericTypeMapping;
    private final ParamConverterProviders paramConverterProviders;
    private final QuarkusRestConfiguration configuration;

    public QuarkusRestDeployment(ExceptionMapping exceptionMapping, ContextResolvers contextResolvers, Serialisers serialisers,
            RestHandler[] abortHandlerChain,
            EntityWriter dynamicEntityWriter, ClientProxies clientProxies, String prefix,
            GenericTypeMapping genericTypeMapping, ParamConverterProviders paramConverterProviders,
            QuarkusRestConfiguration configuration) {
        this.exceptionMapping = exceptionMapping;
        this.contextResolvers = contextResolvers;
        this.serialisers = serialisers;
        this.abortHandlerChain = abortHandlerChain;
        this.dynamicEntityWriter = dynamicEntityWriter;
        this.clientProxies = clientProxies;
        this.prefix = prefix;
        this.genericTypeMapping = genericTypeMapping;
        this.paramConverterProviders = paramConverterProviders;
        this.configuration = configuration;
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

    public Serialisers getSerialisers() {
        return serialisers;
    }

    public RestHandler[] getAbortHandlerChain() {
        return abortHandlerChain;
    }

    public EntityWriter getDynamicEntityWriter() {
        return dynamicEntityWriter;
    }

    public ClientProxies getClientProxies() {
        return clientProxies;
    }

    /**
     * Application path prefix. Must start with "/" and not end with a "/". Cannot be null.
     * 
     * @return the application path prefix, or an empty string.
     */
    public String getPrefix() {
        return prefix;
    }

    public GenericTypeMapping getGenericTypeMapping() {
        return genericTypeMapping;
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
                    return new RuntimePameterConverter(converter);
            }
        }
        return null;
    }
}
