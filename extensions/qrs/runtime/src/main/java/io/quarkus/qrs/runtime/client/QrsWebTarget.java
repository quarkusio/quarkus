package io.quarkus.qrs.runtime.client;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;

import io.quarkus.qrs.runtime.core.Serialisers;
import io.quarkus.qrs.runtime.jaxrs.QrsConfiguration;
import io.quarkus.qrs.runtime.jaxrs.QrsUriBuilder;
import io.vertx.core.http.HttpClient;

public class QrsWebTarget implements WebTarget {

    protected UriBuilder uriBuilder;
    private final HttpClient client;
    private final QrsConfiguration configuration;
    private final Serialisers serialisers;
    private boolean chunked = false;

    public QrsWebTarget(UriBuilder uriBuilder, HttpClient client, Serialisers serialisers) {
        this.uriBuilder = uriBuilder;
        this.client = client;
        this.serialisers = serialisers;
        configuration = new QrsConfiguration();
    }

    public QrsWebTarget(HttpClient client, UriBuilder uriBuilder, QrsConfiguration configuration, Serialisers serialisers) {
        this.client = client;
        this.uriBuilder = uriBuilder;
        this.configuration = configuration;
        this.serialisers = serialisers;
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
        return new QrsUriBuilder().uri(uri);
    }

    private static UriBuilder uriBuilderFromUri(String uri) {
        return new QrsUriBuilder().uri(uri);
    }

    @Override
    public QrsWebTarget clone() {
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
    public Configuration getConfiguration() {
        abortIfClosed();
        return configuration;
    }

    @Override
    public QrsWebTarget path(String path) throws NullPointerException {
        abortIfClosed();
        if (path == null)
            throw new NullPointerException("Param was null");
        UriBuilder copy = uriBuilder.clone().path(path);
        return newInstance(client, copy, configuration);
    }

    @Override
    public QrsWebTarget resolveTemplate(String name, Object value) throws NullPointerException {
        abortIfClosed();
        if (name == null)
            throw new NullPointerException("Param was null");
        if (value == null)
            throw new NullPointerException("Param was null");
        String val = configuration.toString(value);
        UriBuilder copy = uriBuilder.clone().resolveTemplate(name, val);
        QrsWebTarget target = newInstance(client, copy, configuration);
        return target;
    }

    @Override
    public QrsWebTarget resolveTemplates(Map<String, Object> templateValues) throws NullPointerException {
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
        QrsWebTarget target = newInstance(client, copy, configuration);
        return target;
    }

    @Override
    public QrsWebTarget resolveTemplate(String name, Object value, boolean encodeSlashInPath) throws NullPointerException {
        abortIfClosed();
        if (name == null)
            throw new NullPointerException("Param was null");
        if (value == null)
            throw new NullPointerException("Param was null");
        String val = configuration.toString(value);
        UriBuilder copy = uriBuilder.clone().resolveTemplate(name, val, encodeSlashInPath);
        QrsWebTarget target = newInstance(client, copy, configuration);
        return target;
    }

    @Override
    public QrsWebTarget resolveTemplateFromEncoded(String name, Object value) throws NullPointerException {
        abortIfClosed();
        if (name == null)
            throw new NullPointerException("Param was null");
        if (value == null)
            throw new NullPointerException("Param was null");
        String val = configuration.toString(value);
        UriBuilder copy = uriBuilder.clone().resolveTemplateFromEncoded(name, val);
        QrsWebTarget target = newInstance(client, copy, configuration);
        return target;
    }

    @Override
    public QrsWebTarget resolveTemplatesFromEncoded(Map<String, Object> templateValues) throws NullPointerException {
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
        QrsWebTarget target = newInstance(client, copy, configuration);
        return target;
    }

    @Override
    public QrsWebTarget resolveTemplates(Map<String, Object> templateValues, boolean encodeSlashInPath)
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
        QrsWebTarget target = newInstance(client, copy, configuration);
        return target;
    }

    @Override
    public QrsWebTarget matrixParam(String name, Object... values) throws NullPointerException {
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
    public QrsWebTarget queryParam(String name, Object... values) throws NullPointerException {
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

    public QrsWebTarget queryParams(MultivaluedMap<String, Object> parameters)
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

    public QrsWebTarget queryParamNoTemplate(String name, Object... values) throws NullPointerException {
        abortIfClosed();
        if (name == null)
            throw new NullPointerException("Param was null");

        //The whole array can be represented as one object, so we need to cast it to array of objects
        if (values.length == 1 && values[0].getClass().isArray() && !values[0].getClass().getComponentType().isPrimitive()) {
            values = (Object[]) values[0];
        }

        String[] stringValues = toStringValues(values);
        QrsUriBuilder copy;
        if (uriBuilder instanceof QrsUriBuilder) {
            copy = (QrsUriBuilder) uriBuilder.clone();
        } else {
            copy = QrsUriBuilder.fromTemplate(uriBuilder.toTemplate());
        }

        copy.clientQueryParam(name, (Object[]) stringValues);
        return newInstance(client, copy, configuration);
    }

    protected QrsWebTarget newInstance(HttpClient client, UriBuilder uriBuilder, QrsConfiguration configuration) {
        return new QrsWebTarget(client, uriBuilder, configuration, serialisers);
    }

    @Override
    public Invocation.Builder request() {
        abortIfClosed();
        QrsInvocationBuilder builder = createQrsInvocationBuilder(client, uriBuilder, configuration);
        builder.setChunked(chunked);
        builder.setTarget(this);
        return builder;
    }

    @Override
    public Invocation.Builder request(String... acceptedResponseTypes) {
        abortIfClosed();
        QrsInvocationBuilder builder = createQrsInvocationBuilder(client, uriBuilder, configuration);
        builder.getHeaders().accept(acceptedResponseTypes);
        builder.setChunked(chunked);
        builder.setTarget(this);
        return builder;
    }

    @Override
    public Invocation.Builder request(MediaType... acceptedResponseTypes) {
        abortIfClosed();
        QrsInvocationBuilder builder = createQrsInvocationBuilder(client, uriBuilder, configuration);
        builder.getHeaders().accept(acceptedResponseTypes);
        builder.setChunked(chunked);
        builder.setTarget(this);
        return builder;
    }

    private void abortIfClosed() {
        //todo
    }

    protected QrsInvocationBuilder createQrsInvocationBuilder(HttpClient client, UriBuilder uri,
            QrsConfiguration configuration) {
        return new QrsInvocationBuilder(uri.build(), client, configuration, serialisers);
    }

    @Override
    public QrsWebTarget property(String name, Object value) {
        abortIfClosed();
        if (name == null)
            throw new NullPointerException("Param was null");
        configuration.property(name, value);
        return this;
    }

    @Override
    public QrsWebTarget register(Class<?> componentClass) {
        abortIfClosed();
        configuration.register(componentClass);
        return this;
    }

    @Override
    public QrsWebTarget register(Class<?> componentClass, int priority) {
        abortIfClosed();
        configuration.register(componentClass, priority);
        return this;
    }

    @Override
    public QrsWebTarget register(Class<?> componentClass, Class<?>... contracts) {
        abortIfClosed();
        configuration.register(componentClass, contracts);
        return this;
    }

    @Override
    public QrsWebTarget register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        abortIfClosed();
        configuration.register(componentClass, contracts);
        return this;
    }

    @Override
    public QrsWebTarget register(Object component) {
        abortIfClosed();
        configuration.register(component);
        return this;
    }

    @Override
    public QrsWebTarget register(Object component, int priority) {
        abortIfClosed();
        configuration.register(component, priority);
        return this;
    }

    @Override
    public QrsWebTarget register(Object component, Class<?>... contracts) {
        abortIfClosed();
        configuration.register(component, contracts);
        return this;
    }

    @Override
    public QrsWebTarget register(Object component, Map<Class<?>, Integer> contracts) {
        abortIfClosed();
        configuration.register(component, contracts);
        return this;
    }

    public QrsWebTarget setChunked(boolean chunked) {
        this.chunked = chunked;
        return this;
    }
}
