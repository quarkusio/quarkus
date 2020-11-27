package org.jboss.resteasy.reactive.server.providers.serialisers;

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
import org.jboss.resteasy.reactive.common.providers.serialisers.FormUrlEncodedProvider;
import org.jboss.resteasy.reactive.common.providers.serialisers.MessageReaderUtil;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.LazyMethod;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveMessageBodyReader;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveMessageBodyWriter;

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
        implements ResteasyReactiveMessageBodyReader<MultivaluedMap>, ResteasyReactiveMessageBodyWriter<MultivaluedMap> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, LazyMethod lazyMethod, MediaType mediaType) {
        return MultivaluedMap.class.equals(type);
    }

    @Override
    public MultivaluedMap readFrom(Class<MultivaluedMap> type, Type genericType, MediaType mediaType,
            ResteasyReactiveRequestContext context) throws WebApplicationException, IOException {
        return doReadFrom(mediaType, context.getInputStream());
    }

    @Override
    public boolean isWriteable(Class<?> type, LazyMethod target, MediaType mediaType) {
        return MultivaluedMap.class.isAssignableFrom(type);
    }

    @Override
    public void writeResponse(MultivaluedMap o, ResteasyReactiveRequestContext context) throws WebApplicationException {
        try {
            // FIXME: use response encoding
            context.serverResponse().end(multiValuedMapToString(o, MessageReaderUtil.UTF8_CHARSET));
        } catch (UnsupportedEncodingException e) {
            throw new WebApplicationException(e);
        }
    }

}
