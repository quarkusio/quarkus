package org.jboss.resteasy.reactive.server.core.parameters;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.multipart.MultipartSupport;

public class MultipartDataInputExtractor implements ParameterExtractor {

    public static final MultipartDataInputExtractor INSTANCE = new MultipartDataInputExtractor();

    @Override
    public Object extractParameter(ResteasyReactiveRequestContext context) {
        return MultipartSupport.toMultipartFormDataInput(context);
    }
}
