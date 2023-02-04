package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;

@Provider
@Consumes("application/student")
public class GenericResourceStudentReader implements MessageBodyReader<GenericResourceStudent> {

    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    public GenericResourceStudent readFrom(Class<GenericResourceStudent> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(entityStream));
            return new GenericResourceStudent(br.readLine());
        } catch (Exception e) {
            throw new RuntimeException("Unable to parse student.", e);
        }
    }
}
