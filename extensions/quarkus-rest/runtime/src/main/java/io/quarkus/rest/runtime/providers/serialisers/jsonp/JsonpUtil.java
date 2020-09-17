package io.quarkus.rest.runtime.providers.serialisers.jsonp;

import java.io.InputStream;
import java.nio.charset.Charset;

import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.ws.rs.core.MediaType;

import io.quarkus.rest.runtime.providers.serialisers.CharsetUtil;

final class JsonpUtil {

    private static final JsonReaderFactory jsonReaderFactory = Json.createReaderFactory(null);

    static JsonReader reader(InputStream entityStream, MediaType mediaType) {
        return jsonReaderFactory.createReader(entityStream, Charset.forName(CharsetUtil.charsetFromMediaType(mediaType)));
    }
}
