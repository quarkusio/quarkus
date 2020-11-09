package org.jboss.resteasy.reactive.common.model;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyWriter;
import org.jboss.resteasy.reactive.common.util.MediaTypeHelper;
import org.jboss.resteasy.reactive.common.util.ServerMediaType;
import org.jboss.resteasy.reactive.spi.BeanFactory;

public class ResourceWriter {

    private BeanFactory<MessageBodyWriter<?>> factory;
    private List<String> mediaTypeStrings = new ArrayList<>();
    private RuntimeType constraint;
    private boolean builtin = true;
    private volatile List<MediaType> mediaTypes;
    private volatile ServerMediaType serverMediaType;
    private volatile MessageBodyWriter<?> instance;

    public void setFactory(BeanFactory<MessageBodyWriter<?>> factory) {
        this.factory = factory;
    }

    public BeanFactory<MessageBodyWriter<?>> getFactory() {
        return factory;
    }

    public List<String> getMediaTypeStrings() {
        return mediaTypeStrings;
    }

    public ResourceWriter setMediaTypeStrings(List<String> mediaTypeStrings) {
        this.mediaTypeStrings = mediaTypeStrings;
        return this;
    }

    public RuntimeType getConstraint() {
        return constraint;
    }

    public ResourceWriter setConstraint(RuntimeType constraint) {
        this.constraint = constraint;
        return this;
    }

    public boolean isBuiltin() {
        return builtin;
    }

    public void setBuiltin(boolean builtin) {
        this.builtin = builtin;
    }

    public MessageBodyWriter<?> instance() {
        if (instance == null) {
            synchronized (this) {
                if (instance == null) {
                    //todo: manage lifecycle of bean
                    instance = factory.createInstance().getInstance();
                }
            }
        }
        return instance;
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

    public List<MediaType> modifiableMediaTypes() {
        return new ArrayList<>(mediaTypes());
    }

    public ServerMediaType serverMediaType() {
        if (serverMediaType == null) {
            synchronized (this) {
                serverMediaType = new ServerMediaType(mediaTypes(), StandardCharsets.UTF_8.name());
            }
        }
        return serverMediaType;
    }

    public boolean matchesRuntimeType(RuntimeType runtimeType) {
        if (runtimeType == null) {
            return true;
        }
        if (constraint == null) {
            return true;
        }
        return runtimeType == constraint;
    }

    @Override
    public String toString() {
        return "ResourceWriter[constraint: " + constraint + ", mediaTypes: " + mediaTypes + ", factory: " + factory + "]";
    }

    /**
     * The comparison for now is simple:
     * 1) Application provided writers come first
     * 2) Then the more specific the media type, the higher the priority
     * 3) Finally we compare the number of media types
     */
    public static class ResourceWriterComparator implements Comparator<ResourceWriter> {

        public static final ResourceWriterComparator INSTANCE = new ResourceWriterComparator();

        @Override
        public int compare(ResourceWriter o1, ResourceWriter o2) {
            int builtInCompare = Boolean.compare(o1.isBuiltin(), o2.isBuiltin());
            if (builtInCompare != 0) {
                return builtInCompare;
            }

            List<MediaType> mediaTypes1 = o1.mediaTypes();
            List<MediaType> mediaTypes2 = o2.mediaTypes();
            if (mediaTypes1.isEmpty() && mediaTypes2.isEmpty()) {
                return 0;
            }
            if (mediaTypes1.isEmpty()) {
                return 1;
            }
            if (mediaTypes2.isEmpty()) {
                return -1;
            }
            int mediaTypeCompare = MediaTypeHelper.compareWeight(mediaTypes1.get(0), mediaTypes2.get(0));
            if (mediaTypeCompare != 0) {
                return mediaTypeCompare;
            }

            // TODO: not sure if this makes sense but was added to make the sorting more deterministic
            return Integer.compare(mediaTypes1.size(), mediaTypes2.size());
        }
    }
}
