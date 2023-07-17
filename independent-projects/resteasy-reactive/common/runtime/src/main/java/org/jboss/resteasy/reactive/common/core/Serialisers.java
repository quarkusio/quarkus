package org.jboss.resteasy.reactive.common.core;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.WriterInterceptor;

import org.jboss.resteasy.reactive.common.jaxrs.ConfigurationImpl;
import org.jboss.resteasy.reactive.common.model.ResourceReader;
import org.jboss.resteasy.reactive.common.model.ResourceWriter;
import org.jboss.resteasy.reactive.common.util.MediaTypeHelper;
import org.jboss.resteasy.reactive.common.util.QuarkusMultivaluedHashMap;
import org.jboss.resteasy.reactive.common.util.QuarkusMultivaluedMap;

@SuppressWarnings("ForLoopReplaceableByForEach")
public abstract class Serialisers {
    public static final Annotation[] NO_ANNOTATION = new Annotation[0];
    public static final ReaderInterceptor[] NO_READER_INTERCEPTOR = new ReaderInterceptor[0];
    public static final WriterInterceptor[] NO_WRITER_INTERCEPTOR = new WriterInterceptor[0];
    protected static final Map<Class<?>, Class<?>> primitivesToWrappers = new HashMap<>();
    // FIXME: spec says we should use generic type, but not sure how to pass that type from Jandex to reflection
    protected final QuarkusMultivaluedMap<Class<?>, ResourceWriter> writers = new QuarkusMultivaluedHashMap<>();
    protected final QuarkusMultivaluedMap<Class<?>, ResourceReader> readers = new QuarkusMultivaluedHashMap<>();

    public List<MessageBodyReader<?>> findReaders(ConfigurationImpl configuration, Class<?> entityType,
            MediaType mediaType) {
        return findReaders(configuration, entityType, mediaType, null);
    }

    public List<MessageBodyReader<?>> findReaders(ConfigurationImpl configuration, Class<?> entityType,
            MediaType mediaType, RuntimeType runtimeType) {
        List<MediaType> desired = MediaTypeHelper.getUngroupedMediaTypes(mediaType);
        List<MessageBodyReader<?>> ret = new ArrayList<>();
        Deque<Class<?>> toProcess = new LinkedList<>();
        Class<?> klass = entityType;
        if (primitivesToWrappers.containsKey(klass))
            klass = primitivesToWrappers.get(klass);
        QuarkusMultivaluedMap<Class<?>, ResourceReader> readers;
        if (configuration != null && !configuration.getResourceReaders().isEmpty()) {
            readers = new QuarkusMultivaluedHashMap<>();
            readers.addAll(configuration.getResourceReaders());
            readers.addAll(this.readers);
        } else {
            readers = this.readers;
        }
        do {
            Collections.addAll(toProcess, klass.getInterfaces());
            if (klass == Object.class || klass.getSuperclass() == null) {
                //spec extension, look for interfaces as well
                //we match interfaces before Object
                Set<Class<?>> seen = new HashSet<>(toProcess);
                while (!toProcess.isEmpty()) {
                    Class<?> iface = toProcess.poll();
                    List<ResourceReader> goodTypeReaders = readers.get(iface);
                    readerLookup(mediaType, runtimeType, desired, ret, goodTypeReaders);
                    for (Class<?> i : iface.getInterfaces()) {
                        if (!seen.contains(i)) {
                            seen.add(i);
                            toProcess.add(i);
                        }
                    }
                }
            }
            List<ResourceReader> goodTypeReaders = readers.get(klass);
            readerLookup(mediaType, runtimeType, desired, ret, goodTypeReaders);
            if (klass.isInterface()) {
                klass = Object.class;
            } else {
                klass = klass.getSuperclass();
            }
        } while (klass != null);

        return ret;
    }

    private void readerLookup(MediaType mediaType, RuntimeType runtimeType, List<MediaType> desired,
            List<MessageBodyReader<?>> ret,
            List<ResourceReader> goodTypeReaders) {
        if (goodTypeReaders != null && !goodTypeReaders.isEmpty()) {
            List<ResourceReader> mediaTypeMatchingReaders = new ArrayList<>(goodTypeReaders.size());
            for (int i = 0; i < goodTypeReaders.size(); i++) {
                ResourceReader goodTypeReader = goodTypeReaders.get(i);
                if (!goodTypeReader.matchesRuntimeType(runtimeType)) {
                    continue;
                }
                MediaType match = MediaTypeHelper.getFirstMatch(desired, goodTypeReader.mediaTypes());
                if (match != null || mediaType == null) {
                    mediaTypeMatchingReaders.add(goodTypeReader);
                }
            }

            mediaTypeMatchingReaders.sort(new ResourceReader.ResourceReaderComparator(Collections.singletonList(mediaType)));
            for (int i = 0; i < mediaTypeMatchingReaders.size(); i++) {
                ResourceReader mediaTypeMatchingReader = mediaTypeMatchingReaders.get(i);
                ret.add(mediaTypeMatchingReader.instance());
            }
        }
    }

    public <T> void addWriter(Class<T> entityClass, ResourceWriter writer) {
        writers.add(entityClass, writer);
    }

    public <T> void addReader(Class<T> entityClass, ResourceReader reader) {
        readers.add(entityClass, reader);
    }

    public List<MessageBodyWriter<?>> findBuildTimeWriters(Class<?> entityType, RuntimeType runtimeType,
            List<MediaType> produces) {
        if (Response.class.isAssignableFrom(entityType)) {
            return Collections.emptyList();
        }
        Class<?> klass = entityType;
        if (primitivesToWrappers.containsKey(klass))
            klass = primitivesToWrappers.get(klass);
        //first we check to make sure that the return type is build time selectable
        //this fails when there are eligible writers for a sub type of the entity type
        //e.g. if the entity type is Object and there are mappers for String then we
        //can't determine the type at build time
        for (Map.Entry<Class<?>, List<ResourceWriter>> entry : writers.entrySet()) {
            if (klass.isAssignableFrom(entry.getKey()) && !entry.getKey().equals(klass)) {
                //this is a writer registered under a sub type
                //check to see if the media type is relevant
                if (produces == null || produces.isEmpty()) {
                    return null;
                } else {
                    List<ResourceWriter> writers = entry.getValue();
                    for (int i = 0; i < writers.size(); i++) {
                        MediaType match = MediaTypeHelper.getFirstMatch(produces, writers.get(i).mediaTypes());
                        if (match != null) {
                            return null;
                        }
                    }
                }
            }

        }

        var resourceWriters = findResourceWriters(writers, klass, produces, runtimeType);
        // we must NOT sort here because the spec mentions that the writers closer to the requested java type are tried first
        // and the list has already been built up in this way
        return toMessageBodyWriters(resourceWriters);
    }

    protected List<ResourceWriter> findResourceWriters(QuarkusMultivaluedMap<Class<?>, ResourceWriter> writers, Class<?> klass,
            List<MediaType> produces, RuntimeType runtimeType) {
        Class<?> currentClass = klass;
        List<MediaType> desired = MediaTypeHelper.getUngroupedMediaTypes(produces);
        List<ResourceWriter> ret = new ArrayList<>();
        Deque<Class<?>> toProcess = new LinkedList<>();
        do {
            if (currentClass == Object.class) {
                //spec extension, look for interfaces as well
                //we match interfaces before Object
                Set<Class<?>> seen = new HashSet<>(toProcess);
                while (!toProcess.isEmpty()) {
                    Class<?> iface = toProcess.poll();
                    List<ResourceWriter> goodTypeWriters = writers.get(iface);
                    writerLookup(runtimeType, produces, desired, ret, goodTypeWriters);
                    for (Class<?> i : iface.getInterfaces()) {
                        if (!seen.contains(i)) {
                            seen.add(i);
                            toProcess.add(i);
                        }
                    }
                }
            }
            List<ResourceWriter> goodTypeWriters = writers.get(currentClass);
            writerLookup(runtimeType, produces, desired, ret, goodTypeWriters);
            toProcess.addAll(Arrays.asList(currentClass.getInterfaces()));
            // if we're an interface, pretend our superclass is Object to get us through the same logic as a class
            if (currentClass.isInterface()) {
                currentClass = Object.class;
            } else {
                currentClass = currentClass.getSuperclass();
            }
        } while (currentClass != null);

        return ret;
    }

    @SuppressWarnings("rawtypes")
    protected List<MessageBodyWriter<?>> toMessageBodyWriters(List<ResourceWriter> resourceWriters) {
        List<MessageBodyWriter<?>> ret = new ArrayList<>(resourceWriters.size());
        Set<Class<? extends MessageBodyWriter>> alreadySeenClasses = new HashSet<>(resourceWriters.size());
        for (int i = 0; i < resourceWriters.size(); i++) {
            ResourceWriter resourceWriter = resourceWriters.get(i);
            MessageBodyWriter<?> instance = resourceWriter.instance();
            Class<? extends MessageBodyWriter> instanceClass = instance.getClass();
            if (alreadySeenClasses.contains(instanceClass)) {
                continue;
            }
            ret.add(instance);
            alreadySeenClasses.add(instanceClass);
        }
        return ret;
    }

    private void writerLookup(RuntimeType runtimeType, List<MediaType> produces, List<MediaType> desired,
            List<ResourceWriter> ret, List<ResourceWriter> goodTypeWriters) {
        if (goodTypeWriters != null && !goodTypeWriters.isEmpty()) {
            List<ResourceWriter> mediaTypeMatchingWriters = new ArrayList<>(goodTypeWriters.size());

            for (int i = 0; i < goodTypeWriters.size(); i++) {
                ResourceWriter goodTypeWriter = goodTypeWriters.get(i);
                if (!goodTypeWriter.matchesRuntimeType(runtimeType)) {
                    continue;
                }
                MediaType match = MediaTypeHelper.getFirstMatch(desired, goodTypeWriter.mediaTypes());
                if (match != null) {
                    mediaTypeMatchingWriters.add(goodTypeWriter);
                }
            }

            // we sort here because the spec mentions that the writers closer to the requested java type are tried first
            mediaTypeMatchingWriters.sort(new ResourceWriter.ResourceWriterComparator(produces));

            ret.addAll(mediaTypeMatchingWriters);
        }
    }

    public List<MessageBodyWriter<?>> findWriters(ConfigurationImpl configuration, Class<?> entityType,
            MediaType resolvedMediaType) {
        return findWriters(configuration, entityType, resolvedMediaType, null);
    }

    public List<MessageBodyWriter<?>> findWriters(ConfigurationImpl configuration, Class<?> entityType,
            MediaType resolvedMediaType, RuntimeType runtimeType) {
        // FIXME: invocation is very different between client and server, where the server doesn't treat GenericEntity specially
        // it's probably missing from there, while the client handles it upstack
        List<MediaType> mt = Collections.singletonList(resolvedMediaType);
        Class<?> klass = entityType;
        if (primitivesToWrappers.containsKey(klass))
            klass = primitivesToWrappers.get(klass);
        QuarkusMultivaluedMap<Class<?>, ResourceWriter> writers;
        if (configuration != null && !configuration.getResourceWriters().isEmpty()) {
            writers = new QuarkusMultivaluedHashMap<>();
            writers.addAll(configuration.getResourceWriters());
            for (Map.Entry<Class<?>, List<ResourceWriter>> writersEntry : this.writers.entrySet()) {
                writers.addAll(writersEntry.getKey(), writersEntry.getValue());
            }
        } else {
            writers = this.writers;
        }

        return toMessageBodyWriters(findResourceWriters(writers, klass, mt, runtimeType));
    }

    public static class Builtin {
        public final Class<?> entityClass;
        public final String mediaType;
        public final RuntimeType constraint;

        public Builtin(Class<?> entityClass, String mediaType, RuntimeType constraint) {
            this.entityClass = entityClass;
            this.mediaType = mediaType;
            this.constraint = constraint;
        }
    }

    public static class BuiltinWriter extends Builtin {
        public final Class<? extends MessageBodyWriter<?>> writerClass;

        public BuiltinWriter(Class<?> entityClass, Class<? extends MessageBodyWriter<?>> writerClass, String mediaType) {
            this(entityClass, writerClass, mediaType, null);
        }

        public BuiltinWriter(Class<?> entityClass, Class<? extends MessageBodyWriter<?>> writerClass, String mediaType,
                RuntimeType constraint) {
            super(entityClass, mediaType, constraint);
            this.writerClass = writerClass;
        }
    }

    public static class BuiltinReader extends Builtin {
        public final Class<? extends MessageBodyReader<?>> readerClass;

        public BuiltinReader(Class<?> entityClass, Class<? extends MessageBodyReader<?>> readerClass, String mediaType) {
            this(entityClass, readerClass, mediaType, null);
        }

        public BuiltinReader(Class<?> entityClass, Class<? extends MessageBodyReader<?>> readerClass, String mediaType,
                RuntimeType constraint) {
            super(entityClass, mediaType, constraint);
            this.readerClass = readerClass;
        }
    }
}
