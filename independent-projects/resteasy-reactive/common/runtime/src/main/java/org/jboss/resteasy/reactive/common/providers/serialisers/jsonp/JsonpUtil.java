package org.jboss.resteasy.reactive.common.providers.serialisers.jsonp;

import jakarta.json.Json;
import jakarta.json.JsonReader;
import jakarta.json.JsonReaderFactory;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.ws.rs.core.MediaType;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import org.jboss.resteasy.reactive.common.providers.serialisers.MessageReaderUtil;

public final class JsonpUtil {

    private static final JsonReaderFactory jsonReaderFactory = Json.createReaderFactory(null);
    private static final JsonWriterFactory jsonWriterFactory = Json.createWriterFactory(null);

    public static JsonReader reader(InputStream entityStream, MediaType mediaType) {
        return jsonReaderFactory.createReader(entityStream, Charset.forName(MessageReaderUtil.charsetFromMediaType(mediaType)));
    }

    public static JsonWriter writer(OutputStream entityStream, MediaType mediaType) {
        return jsonWriterFactory.createWriter(entityStream, Charset.forName(MessageReaderUtil.charsetFromMediaType(mediaType)));
    }

}
