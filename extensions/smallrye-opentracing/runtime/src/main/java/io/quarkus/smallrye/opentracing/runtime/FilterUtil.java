package io.quarkus.smallrye.opentracing.runtime;

import java.util.HashMap;
import java.util.Map;

import io.opentracing.Span;
import io.opentracing.tag.Tags;

final class FilterUtil {

    private FilterUtil() {
    }

    static void addExceptionLogs(Span span, Throwable throwable) {
        Tags.ERROR.set(span, true);
        if (throwable != null) {
            Map<String, Object> errorLogs = new HashMap<>(2);
            errorLogs.put("event", Tags.ERROR.getKey());
            errorLogs.put("error.object", throwable);
            span.log(errorLogs);
        }
    }
}
