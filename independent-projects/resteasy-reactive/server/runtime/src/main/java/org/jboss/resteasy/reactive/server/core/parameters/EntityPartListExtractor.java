package org.jboss.resteasy.reactive.server.core.parameters;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.multipart.MultipartSupport;

public class EntityPartListExtractor implements ParameterExtractor {

    public static final EntityPartListExtractor INSTANCE = new EntityPartListExtractor();

    @Override
    public Object extractParameter(ResteasyReactiveRequestContext context) {
        return MultipartSupport.toEntityPartList(context);
    }
}
