package io.quarkus.qrs.runtime.headers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;
import javax.ws.rs.ext.MessageBodyWriter;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.quarkus.qrs.runtime.core.QrsRequestContext;
import io.quarkus.qrs.runtime.core.Serialisers;
import io.quarkus.qrs.runtime.core.serialization.FixedEntityWriterArray;
import io.quarkus.qrs.runtime.handlers.RestHandler;
import io.quarkus.qrs.runtime.model.ResourceWriter;
import io.quarkus.qrs.runtime.util.MediaTypeHelper;

/**
 * Handler that negotiates the content type for endpoints that do not specify @Produces
 */
public class NoProducesHandler implements RestHandler {

    public static final List<MediaType> WILDCARD_LIST = Collections.singletonList(MediaType.WILDCARD_TYPE);
    public static final MessageBodyWriter[] EMPTY = new MessageBodyWriter[0];
    private final Serialisers serialisers;

    private final ConcurrentMap<Class<?>, Holder> resourceMap = new ConcurrentHashMap<>();
    private Function<Class<?>, Holder> mappingFunction = new Function<Class<?>, Holder>() {
        @Override
        public Holder apply(Class<?> aClass) {
            Class<?> c = aClass;
            Set<MediaType> types = new LinkedHashSet<>();
            List<ResourceWriter> writers = new ArrayList<>();
            while (c != null) {
                List<ResourceWriter> forClass = serialisers.getWriters().get(c);
                if (forClass != null) {
                    for (ResourceWriter writer : forClass) {
                        types.addAll(writer.mediaTypes());
                        writers.add(writer);
                    }
                }
                for (Class<?> iface : c.getInterfaces()) {
                    forClass = serialisers.getWriters().get(iface);
                    if (forClass != null) {
                        for (ResourceWriter writer : forClass) {
                            types.addAll(writer.mediaTypes());
                            writers.add(writer);
                        }
                    }
                }
                c = c.getSuperclass();
            }
            return new Holder(writers, new ArrayList<>(types));
        }
    };

    public NoProducesHandler(Serialisers serialisers) {
        this.serialisers = serialisers;
    }

    @Override
    public void handle(QrsRequestContext requestContext) throws Exception {
        Object entity = requestContext.getResult();
        if (entity instanceof Response || entity == null) {
            return;
        }
        Holder resultForClass = resourceMap.computeIfAbsent(entity.getClass(), mappingFunction);
        String accept = requestContext.getContext().request().getHeader(HttpHeaderNames.ACCEPT);
        List<MediaType> parsed;
        if (accept != null) {
            //TODO: this needs to be optimised
            parsed = MediaTypeHelper.parseHeader(accept);
        } else {
            parsed = WILDCARD_LIST;
        }
        MediaType res = MediaTypeHelper.getBestMatch(parsed, resultForClass.mediaTypeList);
        if (res == null) {
            requestContext.setThrowable(new WebApplicationException(Response
                    .notAcceptable(Variant
                            .mediaTypes(
                                    resultForClass.mediaTypeList.toArray(new MediaType[resultForClass.mediaTypeList.size()]))
                            .build())
                    .build()));
            return;
        }
        if (res.isWildcardType() || (res.getType().equals("application") && res.isWildcardSubtype())) {
            requestContext.setProducesMediaType(MediaType.APPLICATION_OCTET_STREAM_TYPE);
        } else {
            requestContext.setProducesMediaType(res);
        }
        List<MessageBodyWriter<?>> finalResult = new ArrayList<>();
        for (ResourceWriter i : resultForClass.writers) {
            for (MediaType mt : i.mediaTypes()) {
                if (mt.isCompatible(res)) {
                    finalResult.add(i.getInstance());
                    break;
                }
            }
        }
        //TODO: this is all super yuck
        requestContext.setEntityWriter(new FixedEntityWriterArray(finalResult.toArray(EMPTY)));

    }

    static class Holder {
        final List<ResourceWriter> writers;
        final List<MediaType> mediaTypeList;

        Holder(List<ResourceWriter> writers, List<MediaType> mediaTypeList) {
            this.writers = writers;
            this.mediaTypeList = mediaTypeList;
        }
    }
}
