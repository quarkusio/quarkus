package io.quarkus.rest.runtime.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;

import io.quarkus.rest.runtime.spi.BeanFactory;

public class ResourceContextResolver {

    private BeanFactory<ContextResolver<?>> factory;
    private List<String> mediaTypeStrings = new ArrayList<>();
    private volatile List<MediaType> mediaTypes;

    public void setFactory(BeanFactory<ContextResolver<?>> factory) {
        this.factory = factory;
    }

    public BeanFactory<ContextResolver<?>> getFactory() {
        return factory;
    }

    public ResourceContextResolver setMediaTypeStrings(List<String> mediaTypeStrings) {
        this.mediaTypeStrings = mediaTypeStrings;
        return this;
    }

    public List<MediaType> mediaTypes() {
        if (mediaTypes == null) {
            //todo: does this actually need to be threadsafe?
            synchronized (this) {
                List<MediaType> ret = new ArrayList<>();
                for (String i : mediaTypeStrings) {
                    ret.add(MediaType.valueOf(i));
                }
                mediaTypes = Collections.unmodifiableList(ret);
            }
        }
        return mediaTypes;
    }
}
