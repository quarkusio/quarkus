package io.quarkus.resteasy.common.runtime.providers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.ConstrainedTo;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.plugins.providers.FormUrlEncodedProvider;
import org.jboss.resteasy.resteasy_jaxrs.i18n.LogMessages;
import org.jboss.resteasy.spi.HttpRequest;

/**
 * We maintain a stripped down version of RESTEasy's ServerFormUrlEncodedProvider here because we need a version compatible with
 * our media types discovery i.e. we need a no-args constructor.
 */
@Produces("application/x-www-form-urlencoded")
@Consumes("application/x-www-form-urlencoded")
@ConstrainedTo(RuntimeType.SERVER)
public class ServerFormUrlEncodedProvider extends FormUrlEncodedProvider {

    @Context
    HttpRequest request;

    @SuppressWarnings("rawtypes")
    @Override
    public MultivaluedMap readFrom(Class<MultivaluedMap> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException {
        LogMessages.LOGGER.debugf("Provider : %s,  Method : readFrom", getClass().getName());

        return super.readFrom(type, genericType, annotations, mediaType, httpHeaders, entityStream);
    }
}
