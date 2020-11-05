package io.quarkus.rest.server.runtime.providers.serialisers;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;

import javax.ws.rs.ConstrainedTo;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.reactive.common.runtime.providers.serialisers.FormUrlEncodedProvider;
import org.jboss.resteasy.reactive.common.runtime.providers.serialisers.MessageReaderUtil;

import io.quarkus.rest.server.runtime.core.LazyMethod;
import io.quarkus.rest.server.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.server.runtime.spi.QuarkusRestMessageBodyReader;
import io.quarkus.rest.server.runtime.spi.QuarkusRestMessageBodyWriter;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@SuppressWarnings("rawtypes")
@Provider
@Produces("application/x-www-form-urlencoded")
@Consumes("application/x-www-form-urlencoded")
@ConstrainedTo(RuntimeType.CLIENT)
public class ServerFormUrlEncodedProvider extends FormUrlEncodedProvider
        implements QuarkusRestMessageBodyReader<MultivaluedMap>, QuarkusRestMessageBodyWriter<MultivaluedMap> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, LazyMethod lazyMethod, MediaType mediaType) {
        return MultivaluedMap.class.equals(type);
    }

    @Override
    public MultivaluedMap readFrom(Class<MultivaluedMap> type, Type genericType, MediaType mediaType,
            QuarkusRestRequestContext context) throws WebApplicationException, IOException {
        return doReadFrom(mediaType, context.getInputStream());
    }

    @Override
    public boolean isWriteable(Class<?> type, LazyMethod target, MediaType mediaType) {
        return MultivaluedMap.class.isAssignableFrom(type);
    }

    @Override
    public void writeResponse(MultivaluedMap o, QuarkusRestRequestContext context) throws WebApplicationException {
        try {
            // FIXME: use response encoding
            context.getHttpServerResponse().end(multiValuedMapToString(o, MessageReaderUtil.UTF8_CHARSET));
        } catch (UnsupportedEncodingException e) {
            throw new WebApplicationException(e);
        }
    }

}
