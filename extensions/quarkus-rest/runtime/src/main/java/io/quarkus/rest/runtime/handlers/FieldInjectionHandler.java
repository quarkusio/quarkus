package io.quarkus.rest.runtime.handlers;

import java.util.function.BiConsumer;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.runtime.core.parameters.ParameterExtractor;
import io.quarkus.rest.runtime.core.parameters.converters.ParameterConverter;

public class FieldInjectionHandler implements RestHandler {

    private final BiConsumer<Object, Object> setter;
    private final String defaultValue;
    private final ParameterExtractor extractor;
    private final ParameterConverter converter;

    public FieldInjectionHandler(BiConsumer<Object, Object> setter, String defaultValue, ParameterExtractor extractor,
            ParameterConverter converter) {
        this.setter = setter;
        this.defaultValue = defaultValue;
        this.extractor = extractor;
        this.converter = converter;
    }

    @Override
    public void handle(QuarkusRestRequestContext requestContext) {
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

    private void handleResult(Object result, QuarkusRestRequestContext requestContext) {
        if (result == null) {
            result = defaultValue;
        }
        if (converter != null && result != null) {
            result = converter.convert(result);
        }
        setter.accept(requestContext.getEndpointInstance(), result);
    }
}
