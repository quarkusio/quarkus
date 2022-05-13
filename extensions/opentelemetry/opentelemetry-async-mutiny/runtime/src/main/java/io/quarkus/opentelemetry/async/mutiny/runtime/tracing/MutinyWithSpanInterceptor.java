package io.quarkus.opentelemetry.async.mutiny.runtime.tracing;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import io.opentelemetry.api.OpenTelemetry;

@SuppressWarnings("CdiInterceptorInspection")
@Interceptor
// TODO: We need to run before WithSpanInterceptor. Otherwise, this does not work. What is the approach to achieve that?
//  I did it like this, so that a change to quarkus-opentelemetry is not necessary
@Priority(Interceptor.Priority.PLATFORM_BEFORE - 1)
public class MutinyWithSpanInterceptor {

    public MutinyWithSpanInterceptor(final OpenTelemetry openTelemetry) {

    }

    @AroundInvoke
    public Object span(final InvocationContext invocationContext) throws Exception {
        return null;
    }
}
