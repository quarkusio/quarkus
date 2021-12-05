package org.jboss.resteasy.reactive.common.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.ws.rs.Priorities;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyReader;
import org.jboss.resteasy.reactive.common.util.MediaTypeHelper;
import org.jboss.resteasy.reactive.spi.BeanFactory;

public class ResourceReader {

    private BeanFactory<MessageBodyReader<?>> factory;
    private List<String> mediaTypeStrings = new ArrayList<>();
    private RuntimeType constraint;
    private boolean builtin = true;
    private Integer priority = Priorities.USER;
    private volatile List<MediaType> mediaTypes;
    private volatile MessageBodyReader<?> instance;

    public ResourceReader setFactory(BeanFactory<MessageBodyReader<?>> factory) {
        this.factory = factory;
        return this;
    }

    public BeanFactory<MessageBodyReader<?>> getFactory() {
        return factory;
    }

    public List<String> getMediaTypeStrings() {
        return mediaTypeStrings;
    }

    public ResourceReader setMediaTypeStrings(List<String> mediaTypeStrings) {
        this.mediaTypeStrings = mediaTypeStrings;
        return this;
    }

    public RuntimeType getConstraint() {
        return constraint;
    }

    public ResourceReader setConstraint(RuntimeType constraint) {
        this.constraint = constraint;
        return this;
    }

    public boolean isBuiltin() {
        return builtin;
    }

    public ResourceReader setBuiltin(boolean builtin) {
        this.builtin = builtin;
        return this;
    }

    public Integer getPriority() {
        return priority;
    }

    public ResourceReader setPriority(Integer priority) {
        this.priority = priority;
        return this;
    }

    public MessageBodyReader<?> instance() {
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

    public boolean matchesRuntimeType(RuntimeType runtimeType) {
        if (runtimeType == null) {
            return true;
        }
        if (constraint == null) {
            return true;
        }
        return runtimeType == constraint;
    }

    /**
     * The comparison for now is simple:
     * 1) Application provided writers come first
     * 2) Readers with lower priority come first (same as reader interceptors)
     * 3) Then the more specific the media type, the higher the priority
     * 4) Finally we compare the number of media types
     *
     * The spec doesn't seem to mention this sorting being explicitly needed, but there are tests
     * in the TCK that only pass reliably if the Readers are sorted like this
     */
    public static class ResourceReaderComparator implements Comparator<ResourceReader> {

        public static final ResourceReaderComparator INSTANCE = new ResourceReaderComparator();

        @Override
        public int compare(ResourceReader o1, ResourceReader o2) {
            int builtInCompare = Boolean.compare(o1.isBuiltin(), o2.isBuiltin());
            if (builtInCompare != 0) {
                return builtInCompare;
            }

            int priorityCompare = Integer.compare(o1.getPriority(), o2.getPriority());
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
