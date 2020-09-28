package io.quarkus.rest.runtime.providers.serialisers.jsonp;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.ws.rs.core.MediaType;

import io.quarkus.rest.runtime.providers.serialisers.MessageReaderUtil;

final class JsonpUtil {

    private static final JsonReaderFactory jsonReaderFactory = Json.createReaderFactory(null);
    private static final JsonWriterFactory jsonWriterFactory = Json.createWriterFactory(null);

    static JsonReader reader(InputStream entityStream, MediaType mediaType) {
        return jsonReaderFactory.createReader(entityStream, Charset.forName(MessageReaderUtil.charsetFromMediaType(mediaType)));
    }

    static JsonWriter writer(OutputStream entityStream, MediaType mediaType) {
        return jsonWriterFactory.createWriter(entityStream, Charset.forName(MessageReaderUtil.charsetFromMediaType(mediaType)));
    }

}
