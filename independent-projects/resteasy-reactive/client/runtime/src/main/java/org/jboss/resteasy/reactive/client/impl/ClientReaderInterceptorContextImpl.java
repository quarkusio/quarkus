package org.jboss.resteasy.reactive.client.impl;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.ReaderInterceptorContext;

import org.jboss.resteasy.reactive.client.spi.ClientMessageBodyReader;
import org.jboss.resteasy.reactive.client.spi.MissingMessageBodyReaderErrorMessageContextualizer;
import org.jboss.resteasy.reactive.common.core.Serialisers;
import org.jboss.resteasy.reactive.common.jaxrs.ConfigurationImpl;
import org.jboss.resteasy.reactive.common.util.CaseInsensitiveMap;

public class ClientReaderInterceptorContextImpl extends AbstractClientInterceptorContextImpl
        implements ReaderInterceptorContext {

    private static final List<MissingMessageBodyReaderErrorMessageContextualizer> contextualizers;

    static {
        var loader = ServiceLoader.load(MissingMessageBodyReaderErrorMessageContextualizer.class, Thread.currentThread()
                .getContextClassLoader());
        if (!loader.iterator().hasNext()) {
            contextualizers = Collections.emptyList();
        } else {
            contextualizers = new ArrayList<>(1);
            for (var entry : loader) {
                contextualizers.add(entry);
            }
        }

    }

    final RestClientRequestContext clientRequestContext;
    final ConfigurationImpl configuration;
    final Serialisers serialisers;
    InputStream inputStream;
    private int index = 0;
    private final ReaderInterceptor[] interceptors;
    private final MultivaluedMap<String, String> headers = new CaseInsensitiveMap<>();

    public ClientReaderInterceptorContextImpl(Annotation[] annotations, Class<?> entityClass, Type entityType,
            MediaType mediaType, Map<String, Object> properties,
            RestClientRequestContext clientRequestContext,
            MultivaluedMap<String, String> headers,
            ConfigurationImpl configuration, Serialisers serialisers, InputStream inputStream,
            ReaderInterceptor[] interceptors) {
        super(annotations, entityClass, entityType, mediaType, properties);
        this.clientRequestContext = clientRequestContext;
        this.configuration = configuration;
        this.serialisers = serialisers;
        this.inputStream = inputStream;
        this.interceptors = interceptors;
        this.headers.putAll(headers);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Object proceed() throws IOException, WebApplicationException {
        if (index == interceptors.length) {
            List<MessageBodyReader<?>> readers = serialisers.findReaders(configuration, entityClass, mediaType,
                    RuntimeType.CLIENT);
            for (MessageBodyReader<?> reader : readers) {
                if (reader.isReadable(entityClass, entityType, annotations, mediaType)) {
                    try {
                        if (reader instanceof ClientMessageBodyReader) {
                            return ((ClientMessageBodyReader) reader).readFrom(entityClass, entityType, annotations, mediaType,
                                    headers,
                                    inputStream, clientRequestContext);
                        } else {
                            return ((MessageBodyReader) reader).readFrom(entityClass, entityType, annotations, mediaType,
                                    headers,
                                    inputStream);
                        }

                    } catch (IOException e) {
                        throw new ProcessingException(e);
                    }
                }
            }

            StringBuilder errorMessage = new StringBuilder(
                    "Response could not be mapped to type " + entityType + " for response with media type " + mediaType);
            if (!contextualizers.isEmpty()) {
                var input = new MissingMessageBodyReaderErrorMessageContextualizer.Input() {
                    @Override
                    public Class<?> type() {
                        return entityClass;
                    }

                    @Override
                    public Type genericType() {
                        return entityType;
                    }

                    @Override
                    public Annotation[] annotations() {
                        return annotations;
                    }

                    @Override
                    public MediaType mediaType() {
                        return mediaType;
                    }
                };
                List<String> contextMessages = new ArrayList<>(contextualizers.size());
                for (var contextualizer : contextualizers) {
                    String contextMessage = contextualizer.provideContextMessage(input);
                    if (contextMessage != null) {
                        contextMessages.add(contextMessage);
                    }
                }
                if (!contextMessages.isEmpty()) {
                    errorMessage.append(". Hints: ");
                    errorMessage.append(String.join(",", contextMessages));
                }
            }

            // Spec says to throw this
            throw new ProcessingException(errorMessage.toString());
        } else {
            return interceptors[index++].aroundReadFrom(this);
        }
    }

    @Override
    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public void setInputStream(InputStream is) {
        this.inputStream = is;
    }

    @Override
    public MultivaluedMap<String, String> getHeaders() {
        return headers;
    }

}
