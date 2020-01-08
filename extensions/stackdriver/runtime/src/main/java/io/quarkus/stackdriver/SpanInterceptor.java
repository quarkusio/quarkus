package io.quarkus.stackdriver;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import io.quarkus.stackdriver.runtime.SpanService;

@Interceptor
@Span
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 200)
public class SpanInterceptor {

    private SpanService spanService;

    @Inject
    public SpanInterceptor(SpanService spanService) {
        this.spanService = spanService;
    }

    @AroundInvoke
    public Object computeTrace(InvocationContext invocationCtx) throws Exception {
        return spanService.computeTrace(invocationCtx);
    }
}
