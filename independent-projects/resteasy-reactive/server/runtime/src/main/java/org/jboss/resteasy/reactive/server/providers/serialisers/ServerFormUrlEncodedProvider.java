package org.jboss.resteasy.reactive.server.providers.serialisers;

import jakarta.ws.rs.ConstrainedTo;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import org.jboss.resteasy.reactive.common.providers.serialisers.MapAsFormUrlEncodedProvider;
import org.jboss.resteasy.reactive.common.providers.serialisers.MessageReaderUtil;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyReader;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@SuppressWarnings("rawtypes")
@Provider
@Produces("application/x-www-form-urlencoded")
@Consumes("application/x-www-form-urlencoded")
@ConstrainedTo(RuntimeType.CLIENT)
public class ServerFormUrlEncodedProvider extends MapAsFormUrlEncodedProvider
        implements ServerMessageBodyReader<MultivaluedMap>, ServerMessageBodyWriter<MultivaluedMap> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo lazyMethod, MediaType mediaType) {
        return MultivaluedMap.class.equals(type);
    }

    @Override
    public MultivaluedMap readFrom(Class<MultivaluedMap> type, Type genericType, MediaType mediaType,
            ServerRequestContext context) throws WebApplicationException, IOException {
        return doReadFrom(mediaType, context.getInputStream());
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo target, MediaType mediaType) {
        return MultivaluedMap.class.isAssignableFrom(type);
    }

    @Override
    public void writeResponse(MultivaluedMap o, Type genericType, ServerRequestContext context) throws WebApplicationException {
        try {
            // FIXME: use response encoding
            context.serverResponse().end(multiValuedMapToString(o, MessageReaderUtil.UTF8_CHARSET));
        } catch (UnsupportedEncodingException e) {
            throw new WebApplicationException(e);
        }
    }

}
