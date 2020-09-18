package io.quarkus.rest.runtime.providers.serialisers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import javax.ws.rs.ConstrainedTo;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import io.quarkus.rest.runtime.core.LazyMethod;
import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.runtime.spi.QuarkusRestMessageBodyWriter;
import io.quarkus.rest.runtime.util.Encode;
import io.quarkus.rest.runtime.util.MultivaluedMapImpl;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@SuppressWarnings("rawtypes")
@Provider
@Produces("application/x-www-form-urlencoded")
@Consumes("application/x-www-form-urlencoded")
@ConstrainedTo(RuntimeType.CLIENT)
public class FormUrlEncodedProvider implements MessageBodyReader<MultivaluedMap>, QuarkusRestMessageBodyWriter<MultivaluedMap> {

    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return MultivaluedMap.class.equals(type);
    }

    public MultivaluedMap readFrom(Class<MultivaluedMap> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException {
        String charset = MessageReaderUtil.charsetFromMediaType(mediaType);
        return Encode.decode(parseForm(entityStream, mediaType), charset);
    }

    public static MultivaluedMap<String, String> parseForm(InputStream entityStream, MediaType charset) throws IOException {
        String form = MessageReaderUtil.readString(entityStream, charset);

        MultivaluedMap<String, String> formData = new MultivaluedMapImpl<String, String>();
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

    @Override
    public boolean isWriteable(Class<?> type, LazyMethod target, MediaType mediaType) {
        return MultivaluedMap.class.isAssignableFrom(type);
    }

    @Override
    public void writeResponse(MultivaluedMap o, QuarkusRestRequestContext context) throws WebApplicationException {
        try {
            // FIXME: use response encoding
            context.getContext().response().end(multiValuedMapToString(o, MessageReaderUtil.UTF8_CHARSET));
        } catch (UnsupportedEncodingException e) {
            throw new WebApplicationException(e);
        }
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return MultivaluedMap.class.isAssignableFrom(type);
    }

    @Override
    public void writeTo(MultivaluedMap multivaluedMap, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException {
        String chartSet = MessageReaderUtil.charsetFromMediaType(mediaType);
        entityStream.write(multiValuedMapToString(multivaluedMap, chartSet).getBytes(chartSet));
    }

    private String multiValuedMapToString(MultivaluedMap data, String charset) throws UnsupportedEncodingException {
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
}
