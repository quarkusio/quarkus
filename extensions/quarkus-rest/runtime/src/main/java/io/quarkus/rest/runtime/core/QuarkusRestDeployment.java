package io.quarkus.rest.runtime.core;

import io.quarkus.rest.runtime.client.ClientProxies;
import io.quarkus.rest.runtime.core.serialization.EntityWriter;
import io.quarkus.rest.runtime.handlers.RestHandler;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestConfiguration;

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
}
