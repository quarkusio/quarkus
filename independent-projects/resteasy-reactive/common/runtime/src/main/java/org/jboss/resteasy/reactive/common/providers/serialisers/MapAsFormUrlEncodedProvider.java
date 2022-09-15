package org.jboss.resteasy.reactive.common.providers.serialisers;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import org.jboss.resteasy.reactive.common.util.Encode;
import org.jboss.resteasy.reactive.common.util.QuarkusMultivaluedHashMap;

public class MapAsFormUrlEncodedProvider implements MessageBodyWriter<MultivaluedMap>, MessageBodyReader<MultivaluedMap> {
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return MultivaluedMap.class.equals(type);
    }

    public MultivaluedMap readFrom(Class<MultivaluedMap> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException {
        return doReadFrom(mediaType, entityStream);
    }

    protected MultivaluedMap<String, String> doReadFrom(MediaType mediaType, InputStream entityStream) throws IOException {
        String charset = MessageReaderUtil.charsetFromMediaType(mediaType);
        return Encode.decode(MapAsFormUrlEncodedProvider.parseForm(entityStream, mediaType), charset);
    }

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return MultivaluedMap.class.isAssignableFrom(type);
    }

    public void writeTo(MultivaluedMap multivaluedMap, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException {
        String chartSet = MessageReaderUtil.charsetFromMediaType(mediaType);
        entityStream.write(multiValuedMapToString(multivaluedMap, chartSet).getBytes(chartSet));
    }

    protected String multiValuedMapToString(MultivaluedMap data, String charset) throws UnsupportedEncodingException {
        MultivaluedMap<String, String> formData = (MultivaluedMap<String, String>) data;
        boolean first = true;
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : formData.entrySet()) {
            for (String value : entry.getValue()) {
                if (!first) {
                    sb.append("&");
                }
                value = URLEncoder.encode(value, charset);
                sb.append(entry.getKey());
                sb.append("=");
                sb.append(value);
                first = false;
            }
        }
        return sb.toString();
    }

    public static MultivaluedMap<String, String> parseForm(InputStream entityStream, MediaType charset) throws IOException {
        String form = MessageReaderUtil.readString(entityStream, charset);

        MultivaluedMap<String, String> formData = new QuarkusMultivaluedHashMap<String, String>();
        if ("".equals(form)) {
            return formData;
        }

        String[] params = form.split("&");
        for (String param : params) {
            if (param.indexOf('=') >= 0) {
                String[] nv = param.split("=");
                String val = nv.length > 1 ? nv[1] : "";
                formData.add(nv[0], val);
            } else {
                formData.add(param, "");
            }
        }
        return formData;
    }
}
