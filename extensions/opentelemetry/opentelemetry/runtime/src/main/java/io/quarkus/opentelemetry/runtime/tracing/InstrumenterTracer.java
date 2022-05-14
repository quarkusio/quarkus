package io.quarkus.opentelemetry.runtime.tracing;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.annotation.support.async.AsyncOperationEndSupport;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;

// TODO: The overall idea here is, that it allows to use a custom instrumenter, define a custom parent,
//  which allows you to achieve WithSpan programmatically. Furthermore, it provides more flexibility and makes
//  it easier to create traces but always consider the overall logic, which is able to work sync as well as async
//  for registered strategies. If this makes sense... What do you think?
public class InstrumenterTracer {

    public static <T, REQUEST, RESPONSE> T withSpan(final Instrumenter<REQUEST, RESPONSE> instrumenter,
            final Context parentContext, final REQUEST request, final Class<RESPONSE> responseType,
            final ExceptionSupplier<T> inSpanSupplier) throws Exception {
        return withSpan(instrumenter, parentContext, request, responseType, null, inSpanSupplier);
    }

    public static <T, REQUEST, RESPONSE> T withSpan(final Instrumenter<REQUEST, RESPONSE> instrumenter,
            final Context parentContext, final REQUEST request, final Class<RESPONSE> responseType,
            final Class<T> returnType, final ExceptionSupplier<T> inSpanSupplier) throws Exception {

        final Context currentContext = Context.current();
        final Context actualParentContext = parentContext != null ? parentContext : currentContext;

        final Scope parentScope;
        if (actualParentContext != currentContext) {
            // TODO: overwriting the parent is normally possible in OpenTelemetry. Some tools might ignore it
            //  though. They only would reset to root in case of spanBuilder.setNoParent(). This is not possible via
            //  instrumenter.
            //  Check if there is a different solution. Or maybe the possibility to register some hooks? I donno.
            parentScope = actualParentContext.makeCurrent();
        } else {
            parentScope = Scope.noop();
        }

        try (parentScope) {
            return executeWithSpan(instrumenter, request, responseType, returnType, inSpanSupplier, actualParentContext);
        }
    }

    private static <T, REQUEST, RESPONSE> T executeWithSpan(final Instrumenter<REQUEST, RESPONSE> instrumenter,
            final REQUEST request, final Class<RESPONSE> responseType, final Class<T> returnType,
            final ExceptionSupplier<T> inSpanSupplier, final Context parentContext) throws Exception {
        Context spanContext = null;
        final Scope scope;
        final boolean shouldStart = instrumenter.shouldStart(parentContext, request);
        if (shouldStart) {
            spanContext = instrumenter.start(parentContext, request);
            scope = spanContext.makeCurrent();
        } else {
            scope = Scope.noop();
        }

        try (scope) {
            final T result = inSpanSupplier.get();

            if (shouldStart) {
                return createAsyncEndSupport(instrumenter, responseType, getReturnType(returnType, result)).asyncEnd(
                        spanContext, request, result, null);
            }

            return result;
        } catch (final Exception failure) {
            // async handling not necessary here. In fact not even possible.
            instrumenter.end(spanContext, request, null, failure);
            throw failure;
        }
    }

    private static <T, REQUEST, RESPONSE> AsyncOperationEndSupport<REQUEST, RESPONSE> createAsyncEndSupport(
            final Instrumenter<REQUEST, RESPONSE> instrumenter, final Class<RESPONSE> responseType,
            final Class<T> result) {
        return AsyncOperationEndSupport.create(instrumenter, responseType, result);
    }

    /**
     * We try to determine the return type. Either it is explicitly defined or we check the result class. If result
     * is <code>null</code> we assume <code>Object.class</code> which will not support async behavior at all.
     *
     * @param returnType explicitly defined return type
     * @param result actual result from supplier
     * @return determined return type
     */
    private static Class<?> getReturnType(final Class<?> returnType, final Object result) {
        return returnType != null ? returnType : result != null ? result.getClass() : Object.class;
    }
}
