package io.quarkus.jaeger.runtime;

import io.jaegertracing.Configuration;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;

public class QuarkusJaegerTracer implements Tracer {

    private static volatile Tracer tracer;

    @Override
    public String toString() {
        return tracer().toString();
    }

    private static Tracer tracer() {
        if (tracer == null) {
            synchronized (QuarkusJaegerTracer.class) {
                if (tracer == null) {
                    tracer = Configuration.fromEnv()
                            .withMetricsFactory(new QuarkusJaegerMetricsFactory()).getTracer();
                }
            }
        }
        return tracer;
    }

    @Override
    public SpanBuilder buildSpan(String operationName) {
        return tracer().buildSpan(operationName);
    }

    @Override
    public <C> void inject(SpanContext spanContext, Format<C> format, C carrier) {
        tracer().inject(spanContext, format, carrier);
    }

    @Override
    public <C> SpanContext extract(Format<C> format, C carrier) {
        return tracer().extract(format, carrier);
    }

    @Override
    public ScopeManager scopeManager() {
        return tracer().scopeManager();
    }

    @Override
    public Span activeSpan() {
        return tracer().activeSpan();
    }
}
