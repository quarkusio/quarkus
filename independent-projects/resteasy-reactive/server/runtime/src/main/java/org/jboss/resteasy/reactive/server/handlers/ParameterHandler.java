package org.jboss.resteasy.reactive.server.handlers;

import java.util.Collection;
import java.util.function.BiConsumer;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import org.jboss.resteasy.reactive.common.model.ParameterType;
import org.jboss.resteasy.reactive.common.util.QuarkusRestUtil;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.parameters.ParameterExtractor;
import org.jboss.resteasy.reactive.server.core.parameters.converters.ParameterConverter;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

public class ParameterHandler implements ServerRestHandler {

    private final int index;
    private final String defaultValue;
    private final ParameterExtractor extractor;
    private final ParameterConverter converter;
    private final ParameterType parameterType;
    private final boolean isCollection;

    public ParameterHandler(int index, String defaultValue, ParameterExtractor extractor, ParameterConverter converter,
            ParameterType parameterType, boolean isCollection) {
        this.index = index;
        this.defaultValue = defaultValue;
        this.extractor = extractor;
        this.converter = converter;
        this.parameterType = parameterType;
        this.isCollection = isCollection;
    }

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) {
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

    private void handleResult(Object result, ResteasyReactiveRequestContext requestContext, boolean needsResume) {
        // empty collections should still get their default value
        if (defaultValue != null
                && (result == null || (isCollection && ((Collection) result).isEmpty()))) {
            result = defaultValue;
        }
        Throwable toThrow = null;
        if (converter != null && result != null) {
            // spec says: 
            /*
             * 3.2 Fields and Bean Properties
             * if the field or property is annotated with @MatrixParam, @QueryParam or @PathParam then an implementation
             * MUST generate an instance of NotFoundException (404 status) that wraps the thrown exception and no
             * entity; if the field or property is annotated with @HeaderParam or @CookieParam then an implementation
             * MUST generate an instance of BadRequestException (400 status) that wraps the thrown exception and
             * no entity.
             * 3.3.2 Parameters
             * Exceptions thrown during construction of @FormParam annotated parameter values are treated the same as if
             * the parameter were annotated with @HeaderParam.
             */
            switch (parameterType) {
                case COOKIE:
                case HEADER:
                case FORM:
                    try {
                        result = converter.convert(result);
                    } catch (WebApplicationException x) {
                        toThrow = x;
                    } catch (Throwable x) {
                        toThrow = new BadRequestException(x);
                    }
                    break;
                case MATRIX:
                case PATH:
                case QUERY:
                    try {
                        result = converter.convert(result);
                    } catch (WebApplicationException x) {
                        toThrow = x;
                    } catch (Throwable x) {
                        toThrow = new NotFoundException(x);
                    }
                    break;
                default:
                    try {
                        result = converter.convert(result);
                    } catch (Throwable x) {
                        toThrow = x;
                    }
                    break;
            }
        }
        if (toThrow == null) {
            requestContext.getParameters()[index] = result;
        }
        if (needsResume) {
            if (toThrow == null) {
                requestContext.resume();
            } else {
                requestContext.resume(toThrow);
            }
        } else if (toThrow != null) {
            throw QuarkusRestUtil.sneakyThrow(toThrow);
        }
    }
}
