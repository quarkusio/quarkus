package org.jboss.resteasy.reactive.client.impl;

import io.vertx.core.http.HttpClient;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import org.jboss.resteasy.reactive.client.spi.ClientRestHandler;
import org.jboss.resteasy.reactive.common.core.Serialisers;
import org.jboss.resteasy.reactive.common.jaxrs.ConfigurationImpl;
import org.jboss.resteasy.reactive.common.jaxrs.UriBuilderImpl;
import org.jboss.resteasy.reactive.spi.ThreadSetupAction;

public class WebTargetImpl implements WebTarget {

    protected UriBuilder uriBuilder;
    private final HttpClient client;
    private final ConfigurationImpl configuration;
    private boolean chunked = false;
    private final ClientImpl restClient;
    final HandlerChain handlerChain;
    final ThreadSetupAction requestContext;

    // an additional handler that is passed to the handlerChain
    // used to support observability features
    private ClientRestHandler preClientSendHandler = null;

    public WebTargetImpl(ClientImpl restClient, HttpClient client, UriBuilder uriBuilder,
            ConfigurationImpl configuration,
            HandlerChain handlerChain,
            ThreadSetupAction requestContext) {
        this.restClient = restClient;
        this.client = client;
        this.uriBuilder = uriBuilder;
        this.configuration = configuration;
        this.handlerChain = handlerChain;
        this.requestContext = requestContext;
    }

    /**
     * Get a new UriBuilder explicitly using RESTEasy implementation
     * (instead of running UriBuilder.fromUri(uri) which relies on
     * current registered JAX-RS implementation)
     *
     * @param uri
     * @return
     */
    private static UriBuilder uriBuilderFromUri(URI uri) {
        return new UriBuilderImpl().uri(uri);
    }

    private static UriBuilder uriBuilderFromUri(String uri) {
        return new UriBuilderImpl().uri(uri);
    }

    @Override
    public WebTargetImpl clone() {
        abortIfClosed();
        UriBuilder copy = uriBuilder.clone();
        return newInstance(client, copy, configuration);
    }

    @Override
    public URI getUri() {
        abortIfClosed();
        return uriBuilder.build();
    }

    @Override
    public UriBuilder getUriBuilder() {
        abortIfClosed();
        return uriBuilder.clone();
    }

    @Override
    public ConfigurationImpl getConfiguration() {
        abortIfClosed();
        return configuration;
    }

    @Override
    public WebTargetImpl path(String path) throws NullPointerException {
        abortIfClosed();
        if (path == null)
            throw new NullPointerException("Param was null");
        UriBuilder copy = uriBuilder.clone().path(path);
        return newInstance(client, copy, configuration);
    }

    @Override
    public WebTargetImpl resolveTemplate(String name, Object value) throws NullPointerException {
        abortIfClosed();
        if (name == null)
            throw new NullPointerException("Param was null");
        if (value == null)
            throw new NullPointerException("Param was null");
        String val = configuration.toString(value);
        UriBuilder copy = uriBuilder.clone().resolveTemplate(name, val);
        WebTargetImpl target = newInstance(client, copy, configuration);
        return target;
    }

    @Override
    public WebTargetImpl resolveTemplates(Map<String, Object> templateValues) throws NullPointerException {
        abortIfClosed();
        if (templateValues == null)
            throw new NullPointerException("Param was null");
        if (templateValues.isEmpty())
            return this;
        Map<String, Object> vals = new HashMap<String, Object>();
        for (Map.Entry<String, Object> entry : templateValues.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null)
                throw new NullPointerException("Param was null");
            String val = configuration.toString(entry.getValue());
            vals.put(entry.getKey(), val);
        }
        UriBuilder copy = uriBuilder.clone().resolveTemplates(vals);
        WebTargetImpl target = newInstance(client, copy, configuration);
        return target;
    }

    @Override
    public WebTargetImpl resolveTemplate(String name, Object value, boolean encodeSlashInPath)
            throws NullPointerException {
        abortIfClosed();
        if (name == null)
            throw new NullPointerException("Param was null");
        if (value == null)
            throw new NullPointerException("Param was null");
        String val = configuration.toString(value);
        UriBuilder copy = uriBuilder.clone().resolveTemplate(name, val, encodeSlashInPath);
        WebTargetImpl target = newInstance(client, copy, configuration);
        return target;
    }

    @Override
    public WebTargetImpl resolveTemplateFromEncoded(String name, Object value) throws NullPointerException {
        abortIfClosed();
        if (name == null)
            throw new NullPointerException("Param was null");
        if (value == null)
            throw new NullPointerException("Param was null");
        String val = configuration.toString(value);
        UriBuilder copy = uriBuilder.clone().resolveTemplateFromEncoded(name, val);
        WebTargetImpl target = newInstance(client, copy, configuration);
        return target;
    }

    @Override
    public WebTargetImpl resolveTemplatesFromEncoded(Map<String, Object> templateValues) throws NullPointerException {
        abortIfClosed();
        if (templateValues == null)
            throw new NullPointerException("Param was null");
        if (templateValues.isEmpty())
            return this;
        Map<String, Object> vals = new HashMap<String, Object>();
        for (Map.Entry<String, Object> entry : templateValues.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null)
                throw new NullPointerException("Param was null");
            String val = configuration.toString(entry.getValue());
            vals.put(entry.getKey(), val);
        }
        UriBuilder copy = uriBuilder.clone().resolveTemplatesFromEncoded(vals);
        WebTargetImpl target = newInstance(client, copy, configuration);
        return target;
    }

    @Override
    public WebTargetImpl resolveTemplates(Map<String, Object> templateValues, boolean encodeSlashInPath)
            throws NullPointerException {
        abortIfClosed();
        if (templateValues == null)
            throw new NullPointerException("Param was null");
        if (templateValues.isEmpty())
            return this;
        Map<String, Object> vals = new HashMap<String, Object>();
        for (Map.Entry<String, Object> entry : templateValues.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null)
                throw new NullPointerException("Param was null");
            String val = configuration.toString(entry.getValue());
            vals.put(entry.getKey(), val);
        }
        UriBuilder copy = uriBuilder.clone().resolveTemplates(vals, encodeSlashInPath);
        WebTargetImpl target = newInstance(client, copy, configuration);
        return target;
    }

    @Override
    public WebTargetImpl matrixParam(String name, Object... values) throws NullPointerException {
        abortIfClosed();
        if (name == null)
            throw new NullPointerException("Param was null");
        UriBuilder copy = uriBuilder.clone();
        if (values.length == 1 && values[0] == null) {
            copy.replaceMatrixParam(name, (Object[]) null);
        } else {
            String[] stringValues = toStringValues(values);
            copy.matrixParam(name, (Object[]) stringValues);
        }
        return newInstance(client, copy, configuration);
    }

    private String[] toStringValues(Object[] values) {
        String[] stringValues = new String[values.length];
        for (int i = 0; i < stringValues.length; i++) {
            stringValues[i] = configuration.toString(values[i]);
        }
        return stringValues;
    }

    @Override
    public WebTargetImpl queryParam(String name, Object... values) throws NullPointerException {
        abortIfClosed();
        if (name == null)
            throw new NullPointerException("Param was null");
        UriBuilder copy = uriBuilder.clone();
        if (values == null || (values.length == 1 && values[0] == null)) {
            copy.replaceQueryParam(name, (Object[]) null);
        } else {
            String[] stringValues = toStringValues(values);
            copy.queryParam(name, (Object[]) stringValues);
        }
        return newInstance(client, copy, configuration);
    }

    public WebTargetImpl queryParams(MultivaluedMap<String, Object> parameters)
            throws IllegalArgumentException, NullPointerException {
        abortIfClosed();
        if (parameters == null)
            throw new NullPointerException("Param was null");
        UriBuilder copy = uriBuilder.clone();
        for (Map.Entry<String, List<Object>> entry : parameters.entrySet()) {
            String[] stringValues = toStringValues(entry.getValue().toArray());
            copy.queryParam(entry.getKey(), (Object[]) stringValues);
        }
        return newInstance(client, copy, configuration);
    }

    public WebTargetImpl queryParamNoTemplate(String name, Object... values) throws NullPointerException {
        abortIfClosed();
        if (name == null)
            throw new NullPointerException("Param was null");

        //The whole array can be represented as one object, so we need to cast it to array of objects
        if (values.length == 1 && values[0].getClass().isArray() && !values[0].getClass().getComponentType().isPrimitive()) {
            values = (Object[]) values[0];
        }

        String[] stringValues = toStringValues(values);
        UriBuilderImpl copy;
        if (uriBuilder instanceof UriBuilderImpl) {
            copy = (UriBuilderImpl) uriBuilder.clone();
        } else {
            copy = UriBuilderImpl.fromTemplate(uriBuilder.toTemplate());
        }

        copy.clientQueryParam(name, (Object[]) stringValues);
        return newInstance(client, copy, configuration);
    }

    protected WebTargetImpl newInstance(HttpClient client, UriBuilder uriBuilder,
            ConfigurationImpl configuration) {
        WebTargetImpl result = new WebTargetImpl(restClient, client, uriBuilder, configuration,
                handlerChain.setPreClientSendHandler(preClientSendHandler),
                requestContext);
        result.setPreClientSendHandler(preClientSendHandler);
        return result;
    }

    @Override
    public Invocation.Builder request() {
        abortIfClosed();
        InvocationBuilderImpl builder = createQuarkusRestInvocationBuilder(client, uriBuilder, configuration);
        builder.setChunked(chunked);
        return builder;
    }

    @Override
    public Invocation.Builder request(String... acceptedResponseTypes) {
        abortIfClosed();
        InvocationBuilderImpl builder = createQuarkusRestInvocationBuilder(client, uriBuilder, configuration);
        builder.getHeaders().accept(acceptedResponseTypes);
        builder.setChunked(chunked);
        return builder;
    }

    @Override
    public Invocation.Builder request(MediaType... acceptedResponseTypes) {
        abortIfClosed();
        InvocationBuilderImpl builder = createQuarkusRestInvocationBuilder(client, uriBuilder, configuration);
        builder.getHeaders().accept(acceptedResponseTypes);
        builder.setChunked(chunked);
        return builder;
    }

    private void abortIfClosed() {
        restClient.abortIfClosed();
    }

    protected InvocationBuilderImpl createQuarkusRestInvocationBuilder(HttpClient client, UriBuilder uri,
            ConfigurationImpl configuration) {
        return new InvocationBuilderImpl(uri.build(), restClient, client, this, configuration,
                handlerChain.setPreClientSendHandler(preClientSendHandler), requestContext);
    }

    @Override
    public WebTargetImpl property(String name, Object value) {
        abortIfClosed();
        if (name == null)
            throw new NullPointerException("Param was null");
        configuration.property(name, value);
        return this;
    }

    @Override
    public WebTargetImpl register(Class<?> componentClass) {
        abortIfClosed();
        configuration.register(componentClass);
        return this;
    }

    @Override
    public WebTargetImpl register(Class<?> componentClass, int priority) {
        abortIfClosed();
        configuration.register(componentClass, priority);
        return this;
    }

    @Override
    public WebTargetImpl register(Class<?> componentClass, Class<?>... contracts) {
        abortIfClosed();
        configuration.register(componentClass, contracts);
        return this;
    }

    @Override
    public WebTargetImpl register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        abortIfClosed();
        configuration.register(componentClass, contracts);
        return this;
    }

    @Override
    public WebTargetImpl register(Object component) {
        abortIfClosed();
        configuration.register(component);
        return this;
    }

    @Override
    public WebTargetImpl register(Object component, int priority) {
        abortIfClosed();
        configuration.register(component, priority);
        return this;
    }

    @Override
    public WebTargetImpl register(Object component, Class<?>... contracts) {
        abortIfClosed();
        configuration.register(component, contracts);
        return this;
    }

    @Override
    public WebTargetImpl register(Object component, Map<Class<?>, Integer> contracts) {
        abortIfClosed();
        configuration.register(component, contracts);
        return this;
    }

    public WebTargetImpl setChunked(boolean chunked) {
        this.chunked = chunked;
        return this;
    }

    public <T> T proxy(Class<?> clazz) {
        return restClient.getClientContext().getClientProxies().get(clazz, this);
    }

    public ClientImpl getRestClient() {
        return restClient;
    }

    @SuppressWarnings("unused")
    public void setPreClientSendHandler(ClientRestHandler preClientSendHandler) {
        this.preClientSendHandler = preClientSendHandler;
    }

    Serialisers getSerialisers() {
        return restClient.getClientContext().getSerialisers();
    }
}
