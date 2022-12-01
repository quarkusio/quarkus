package io.quarkus.jaeger.runtime;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;

import org.jboss.logging.Logger;

import io.jaegertracing.Configuration;
import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.spi.MetricsFactory;
import io.jaegertracing.spi.Reporter;
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
    private boolean zipkinCompatibilityMode = false;
    private String endpoint = null;

    private static final Logger log = Logger.getLogger(QuarkusJaegerTracer.class);

    private final ScopeManager scopeManager = new ScopeManager() {

        volatile ScopeManager delegate;

        @Override
        public Scope activate(Span span) {
            return sm().activate(span);
        }

        @Override
        public Span activeSpan() {
            if (delegate == null) {
                return null;
            }
            return sm().activeSpan();
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
                            .withReporter(createReporter())
                            .build();
                }
            }
        }
        return tracer;
    }

    private Reporter createReporter() {
        Reporter reporter = null;
        if (zipkinCompatibilityMode) {
            Instance<ReporterFactory> registries = CDI.current().select(ReporterFactory.class,
                    Default.Literal.INSTANCE);
            ReporterFactory factory = null;
            if (registries.isAmbiguous()) {
                factory = registries.iterator().next();
                log.warnf("Multiple reporters present, using %s", factory.getClass().getName());
            } else if (!registries.isUnsatisfied()) {
                factory = registries.get();
            }
            if (factory != null) {
                reporter = factory.createReporter(endpoint);
            }
        }
        return reporter;
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
    public void close() {
        tracer.close();
    }

    @Override
    public ScopeManager scopeManager() {
        return scopeManager;
    }

    @Override
    public Span activeSpan() {
        return tracer().activeSpan();
    }

    @Override
    public Scope activateSpan(final Span span) {
        return tracer.activateSpan(span);
    }

    public void setZipkinCompatibilityMode(boolean zipkinCompatibilityMode) {
        this.zipkinCompatibilityMode = zipkinCompatibilityMode;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
}
