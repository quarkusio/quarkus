package io.quarkus.tika.runtime.jaxrs;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyReader;

public abstract class AbstractTikaReader<T> implements MessageBodyReader<T> {
    private Class<T> tikaClass;

    protected AbstractTikaReader(Class<T> tikaClass) {
        this.tikaClass = tikaClass;
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return tikaClass == type;
    }

}
