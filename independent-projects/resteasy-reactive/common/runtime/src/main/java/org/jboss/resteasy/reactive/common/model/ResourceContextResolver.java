package org.jboss.resteasy.reactive.common.model;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.ContextResolver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jboss.resteasy.reactive.spi.BeanFactory;

public class ResourceContextResolver {

    private BeanFactory<ContextResolver<?>> factory;
    private String className;
    private List<String> mediaTypeStrings = new ArrayList<>();
    private volatile List<MediaType> mediaTypes;

    public void setFactory(BeanFactory<ContextResolver<?>> factory) {
        this.factory = factory;
    }

    public BeanFactory<ContextResolver<?>> getFactory() {
        return factory;
    }

    public String getClassName() {
        return className;
    }

    public ResourceContextResolver setClassName(String className) {
        this.className = className;
        return this;
    }

    public ResourceContextResolver setMediaTypeStrings(List<String> mediaTypeStrings) {
        this.mediaTypeStrings = mediaTypeStrings;
        return this;
    }

    public List<String> getMediaTypeStrings() {
        return mediaTypeStrings;
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
