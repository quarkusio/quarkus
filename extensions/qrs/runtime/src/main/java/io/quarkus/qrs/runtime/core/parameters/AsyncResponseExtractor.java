package io.quarkus.qrs.runtime.core.parameters;

import io.quarkus.qrs.runtime.core.QrsRequestContext;
import io.quarkus.qrs.runtime.jaxrs.QrsAsyncResponse;

public class AsyncResponseExtractor implements ParameterExtractor {

    public static final AsyncResponseExtractor INSTANCE = new AsyncResponseExtractor();

    @Override
    public Object extractParameter(QrsRequestContext context) {
        QrsAsyncResponse response = new QrsAsyncResponse(context);
        context.setAsyncResponse(response);
        return response;
    }

}
