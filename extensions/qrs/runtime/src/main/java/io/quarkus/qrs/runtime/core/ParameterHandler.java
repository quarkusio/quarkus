package io.quarkus.qrs.runtime.core;

import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;

public class ParameterHandler implements RestHandler {

    private final int index;
    private final ParameterExtractor extractor;
    private final ParameterConverter converter;

    public ParameterHandler(int index, ParameterExtractor extractor, ParameterConverter converter) {
        this.index = index;
        this.extractor = extractor;
        this.converter = converter;
    }

    @Override
    public void handle(RequestContext requestContext) {
        Object result = extractor.extractParameter(requestContext);
        if (result instanceof CompletionStage<?>) {
            requestContext.suspend();
            ((CompletionStage<?>) result).handle(new BiFunction<Object, Throwable, Object>() {
                @Override
                public Object apply(Object o, Throwable throwable) {
                    if (throwable != null) {
                        requestContext.resume(throwable);
                    } else {
                        handleResult(o, requestContext);
                        requestContext.resume();
                    }
                    return null;
                }
            });
        } else {
            handleResult(result, requestContext);
        }
    }

    private void handleResult(Object result, RequestContext requestContext) {
        if (converter != null) {
            result = converter.convert(result);
        }
        requestContext.getParameters()[index] = result;
    }
}
