package org.jboss.resteasy.reactive.server.jackson;

import static org.jboss.resteasy.reactive.common.providers.serialisers.JsonMessageBodyWriterUtil.setContentTypeIfNecessary;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.reactive.server.StreamingOutputStream;

import com.fasterxml.jackson.annotation.JsonView;

import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.ObjectWriter;

public final class JacksonMessageBodyWriterUtil {

    private JacksonMessageBodyWriterUtil() {
    }

    public static ObjectWriter createDefaultWriter(ObjectMapper mapper) {
        // we don't want the ObjectWriter to close the stream automatically, as we want to handle closing manually at the proper points
        return setNecessaryWriteConfig(mapper.writer());
    }

    public static ObjectWriter setNecessaryWriteConfig(ObjectWriter writer) {
        return writer
                .without(StreamWriteFeature.AUTO_CLOSE_TARGET)
                .without(StreamWriteFeature.FLUSH_PASSED_TO_STREAM);
    }

    public static ObjectReader setNecessaryReadConfig(ObjectReader reader) {
        return reader.without(StreamReadFeature.AUTO_CLOSE_SOURCE);
    }

    public static void doLegacyWrite(Object o, Annotation[] annotations, MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream, ObjectWriter defaultWriter) throws IOException {
        setContentTypeIfNecessary(httpHeaders);
        if ((o instanceof String) && (!(entityStream instanceof StreamingOutputStream))) {
            // YUK: done in order to avoid adding extra quotes... when we are not streaming a result
            entityStream.write(((String) o).getBytes(StandardCharsets.UTF_8));
        } else {
            if (annotations != null) {
                for (Annotation annotation : annotations) {
                    if (JsonView.class.equals(annotation.annotationType())) {
                        if (handleJsonView(((JsonView) annotation), o, entityStream, defaultWriter)) {
                            return;
                        }
                    }
                }
            }
            entityStream.write(defaultWriter.writeValueAsString(o).getBytes(StandardCharsets.UTF_8));
        }
    }

    private static boolean handleJsonView(JsonView jsonView, Object o, OutputStream stream, ObjectWriter defaultWriter)
            throws IOException {
        if ((jsonView != null) && (jsonView.value().length > 0)) {
            defaultWriter.withView(jsonView.value()[0]).writeValue(stream, o);
            return true;
        }
        return false;
    }
}
