package org.jboss.resteasy.reactive.common.model;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.ws.rs.Priorities;
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
    private Integer priority = Priorities.USER;
    private volatile List<MediaType> mediaTypes;
    private volatile ServerMediaType serverMediaType;
    private volatile MessageBodyWriter<?> instance;

    public ResourceWriter setFactory(BeanFactory<MessageBodyWriter<?>> factory) {
        this.factory = factory;
        return this;
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

    public ResourceWriter setBuiltin(boolean builtin) {
        this.builtin = builtin;
        return this;
    }

    public Integer getPriority() {
        return priority;
    }

    public ResourceWriter setPriority(Integer priority) {
        this.priority = priority;
        return this;
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
                List<MediaType> mts = new ArrayList<>(mediaTypeStrings.size());
                for (int i = 0; i < mediaTypeStrings.size(); i++) {
                    mts.add(MediaType.valueOf(mediaTypeStrings.get(i)));
                }
                mediaTypes = Collections.unmodifiableList(mts);
            }
        }
        return mediaTypes;
    }

    public ServerMediaType serverMediaType() {
        if (serverMediaType == null) {
            synchronized (this) {
                // a MessageBodyWriter should always return its configured media type when negotiating, hence the 'false' for 'useSuffix'
                serverMediaType = new ServerMediaType(mediaTypes(), StandardCharsets.UTF_8.name(), false, false);
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
     * 2) Writers higher priority come first (same as writer interceptors)
     * 3) Then the more specific the media type, the higher the priority
     * 4) Finally we compare the number of media types
     */
    public static class ResourceWriterComparator implements Comparator<ResourceWriter> {

        public static final ResourceWriterComparator INSTANCE = new ResourceWriterComparator();

        @Override
        public int compare(ResourceWriter o1, ResourceWriter o2) {
            int builtInCompare = Boolean.compare(o1.isBuiltin(), o2.isBuiltin());
            if (builtInCompare != 0) {
                return builtInCompare;
            }

            int priorityCompare = Integer.compare(o2.getPriority(), o1.getPriority());
            if (priorityCompare != 0) {
                return priorityCompare;
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

            // done to make the sorting result deterministic
            return Integer.compare(mediaTypes1.size(), mediaTypes2.size());
        }
    }
}
