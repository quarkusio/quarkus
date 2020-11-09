package org.jboss.resteasy.reactive.common.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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
    private volatile List<MediaType> mediaTypes;
    private volatile MessageBodyReader<?> instance;

    public void setFactory(BeanFactory<MessageBodyReader<?>> factory) {
        this.factory = factory;
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

    public void setBuiltin(boolean builtin) {
        this.builtin = builtin;
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
                List<MediaType> ret = new ArrayList<>();
                for (String i : mediaTypeStrings) {
                    ret.add(MediaType.valueOf(i));
                }
                mediaTypes = Collections.unmodifiableList(ret);
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
     * 2) Then the more specific the media type, the higher the priority
     * 3) Finally we compare the number of media types
     *
     * The spec doesn't seem to mention this sorting being explicitly needed, but there are tests
     * in the TCK that only pass reliably if the Readers are sorted like this
     *
     * TODO: if this actually follows the exact same rules as ResourceWriter, we need to refactor
     */
    public static class ResourceReaderComparator implements Comparator<ResourceReader> {

        public static final ResourceReaderComparator INSTANCE = new ResourceReaderComparator();

        @Override
        public int compare(ResourceReader o1, ResourceReader o2) {
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
