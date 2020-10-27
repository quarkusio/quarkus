package io.quarkus.rest.server.test.resource.basic.resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

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
