package io.quarkus.resteasy.reactive.jackson.runtime.serialisers;

import static org.jboss.resteasy.reactive.common.providers.serialisers.JsonMessageBodyWriterUtil.setContentTypeIfNecessary;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.quarkus.resteasy.reactive.jackson.CustomSerialization;

public class JacksonBasicMessageBodyWriter implements MessageBodyWriter<Object> {

    private static final String JSON_VIEW_NAME = JsonView.class.getName();
    private static final String CUSTOM_SERIALIZATION = CustomSerialization.class.getName();

    protected final ObjectMapper originalMapper;
    protected final ObjectWriter defaultWriter;
    protected final ConcurrentMap<Method, ObjectWriter> perMethodWriter = new ConcurrentHashMap<>();

    @Inject
    public JacksonBasicMessageBodyWriter(ObjectMapper mapper) {
        this.originalMapper = mapper;
        // we don't want the ObjectWriter to close the stream automatically, as we want to handle closing manually at the proper points
        JsonFactory jsonFactory = mapper.getFactory();
        if (needsNewFactory(jsonFactory)) {
            jsonFactory = jsonFactory.copy();
            setNecessaryJsonFactoryConfig(jsonFactory);
            this.defaultWriter = mapper.writer().with(jsonFactory);
        } else {
            this.defaultWriter = mapper.writer();
        }
    }

    private boolean needsNewFactory(JsonFactory jsonFactory) {
        return jsonFactory.isEnabled(Feature.AUTO_CLOSE_TARGET) || jsonFactory.isEnabled(Feature.FLUSH_PASSED_TO_STREAM);
    }

    protected static void setNecessaryJsonFactoryConfig(JsonFactory jsonFactory) {
        jsonFactory.configure(Feature.AUTO_CLOSE_TARGET, false);
        jsonFactory.configure(Feature.FLUSH_PASSED_TO_STREAM, false);
    }

    @Override
    public boolean isWriteable(Class type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public void writeTo(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        setContentTypeIfNecessary(httpHeaders);
        if (o instanceof String) { // YUK: done in order to avoid adding extra quotes...
            entityStream.write(((String) o).getBytes());
        } else {
            if (annotations != null) {
                for (Annotation annotation : annotations) {
                    if (JsonView.class.equals(annotation.annotationType())) {
                        if (handleJsonView(((JsonView) annotation), o, entityStream)) {
                            return;
                        }
                    }
                }
            }
            entityStream.write(defaultWriter.writeValueAsBytes(o));
        }
    }

    private boolean handleJsonView(JsonView jsonView, Object o, OutputStream stream) throws IOException {
        if ((jsonView != null) && (jsonView.value().length > 0)) {
            defaultWriter.withView(jsonView.value()[0]).writeValue(stream, o);
            return true;
        }
        return false;
    }
}
