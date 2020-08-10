package io.quarkus.qrs.runtime.handlers;

import java.util.function.BiConsumer;

import io.quarkus.qrs.runtime.core.QrsRequestContext;
import io.quarkus.qrs.runtime.core.parameters.ParameterExtractor;
import io.quarkus.qrs.runtime.core.parameters.converters.ParameterConverter;

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
    public void handle(QrsRequestContext requestContext) {
        Object result = extractor.extractParameter(requestContext);
        if (result instanceof ParameterExtractor.ParameterCallback) {
            requestContext.suspend();
            ((ParameterExtractor.ParameterCallback) result).setListener(new BiConsumer<Object, Exception>() {
                @Override
                public void accept(Object o, Exception throwable) {
                    if (throwable != null) {
                        requestContext.resume(throwable);
                    } else {
                        handleResult(o, requestContext);
                        requestContext.resume();
                    }
                }
            });
        } else {
            handleResult(result, requestContext);
        }
    }

    private void handleResult(Object result, QrsRequestContext requestContext) {
        if (converter != null) {
            result = converter.convert(result);
        }
        requestContext.getParameters()[index] = result;
    }
}
