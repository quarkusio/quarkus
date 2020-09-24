package io.quarkus.rest.runtime.handlers;

import java.util.function.BiConsumer;

import javax.ws.rs.WebApplicationException;

import io.quarkus.arc.ClientProxy;
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
        try {
            Object result = extractor.extractParameter(requestContext);
            if (result instanceof ParameterExtractor.ParameterCallback) {
                requestContext.suspend();
                ((ParameterExtractor.ParameterCallback) result).setListener(new BiConsumer<Object, Exception>() {
                    @Override
                    public void accept(Object o, Exception throwable) {
                        if (throwable != null) {
                            requestContext.resume(throwable);
                        } else {
                            handleResult(o, requestContext, true);
                        }
                    }
                });
            } else {
                handleResult(result, requestContext, false);
            }
        } catch (Exception e) {
            if (e instanceof WebApplicationException) {
                throw e;
            } else {
                throw new WebApplicationException(e, 400);
            }
        }
    }

    private void handleResult(Object result, QuarkusRestRequestContext requestContext, boolean needsResume) {

        try {
            if (result == null) {
                result = defaultValue;
            }
            if (converter != null && result != null) {
                result = converter.convert(result);
            }
            Object endpointInstance = requestContext.getEndpointInstance();
            if (endpointInstance instanceof ClientProxy) {
                endpointInstance = ((ClientProxy) endpointInstance).arc_contextualInstance();
            }
            setter.accept(endpointInstance, result);
            if (needsResume) {
                requestContext.resume();
            }

        } catch (Exception e) {
            WebApplicationException toThrow;
            if (e instanceof WebApplicationException) {
                toThrow = (WebApplicationException) e;
            } else {
                toThrow = new WebApplicationException(e, 400);
            }
            if (needsResume) {
                requestContext.resume(toThrow);
            } else {
                throw toThrow;
            }
        }
    }
}
