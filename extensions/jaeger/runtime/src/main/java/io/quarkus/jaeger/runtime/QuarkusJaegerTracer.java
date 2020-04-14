package io.quarkus.jaeger.runtime;

import io.jaegertracing.Configuration;
import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.metrics.NoopMetricsFactory;
import io.jaegertracing.spi.MetricsFactory;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.util.ThreadLocalScopeManager;

public class QuarkusJaegerTracer implements Tracer {

    private volatile JaegerTracer tracer;

    private boolean logTraceContext;
    private boolean metricsEnabled;

    void setLogTraceContext(boolean logTraceContext) {
        this.logTraceContext = logTraceContext;
    }

    void setMetricsEnabled(boolean metricsEnabled) {
        this.metricsEnabled = metricsEnabled;
    }

    @Override
    public String toString() {
        return tracer().toString();
    }

    synchronized void reset() {
        if (tracer != null) {
            tracer.close();
        }
        tracer = null;
    }

    private Tracer tracer() {
        if (tracer == null) {
            synchronized (this) {
                if (tracer == null) {
                    MetricsFactory metricsFactory = metricsEnabled
                            ? new QuarkusJaegerMetricsFactory()
                            : new NoopMetricsFactory();
                    tracer = Configuration.fromEnv()
                            .withMetricsFactory(metricsFactory)
                            .getTracerBuilder()
                            .withScopeManager(getScopeManager())
                            .build();
                }
            }
        }
        return tracer;
    }

    private ScopeManager getScopeManager() {
        ScopeManager scopeManager = new ThreadLocalScopeManager();
        if (logTraceContext) {
            scopeManager = new MDCScopeManager(scopeManager);
        }
        return scopeManager;
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
