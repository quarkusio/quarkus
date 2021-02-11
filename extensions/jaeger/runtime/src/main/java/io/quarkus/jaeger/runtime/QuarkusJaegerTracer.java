package io.quarkus.jaeger.runtime;

import io.jaegertracing.Configuration;
import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.spi.MetricsFactory;
import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.util.ThreadLocalScopeManager;

public class QuarkusJaegerTracer implements Tracer {

    private volatile JaegerTracer tracer;

    private boolean logTraceContext;
    private MetricsFactory metricsFactory;

    private final ScopeManager scopeManager = new ScopeManager() {

        volatile ScopeManager delegate;

        @Override
        public Scope activate(Span span, boolean b) {
            return sm().activate(span, b);
        }

        @Override
        public Scope active() {
            if (delegate == null) {
                return null;
            }
            return sm().active();
        }

        ScopeManager sm() {
            if (delegate == null) {
                synchronized (this) {
                    if (delegate == null) {
                        delegate = getScopeManager();
                    }
                }
            }
            return delegate;
        }
    };

    void setLogTraceContext(boolean logTraceContext) {
        this.logTraceContext = logTraceContext;
    }

    void setMetricsFactory(MetricsFactory metricsFactory) {
        this.metricsFactory = metricsFactory;
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
                    tracer = Configuration.fromEnv()
                            .withMetricsFactory(metricsFactory)
                            .getTracerBuilder()
                            .withScopeManager(scopeManager)
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
        return scopeManager;
    }

    @Override
    public Span activeSpan() {
        return tracer().activeSpan();
    }
}
