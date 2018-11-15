package org.jboss.shamrock.opentracing.runtime;

import java.util.concurrent.atomic.AtomicReference;

import com.uber.jaeger.Configuration;
import com.uber.jaeger.senders.UdpSender;
import io.opentracing.ActiveSpan;
import io.opentracing.NoopTracer;
import io.opentracing.NoopTracerFactory;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;

import static com.uber.jaeger.Configuration.JAEGER_SERVICE_NAME;

/**
 * Created by bob on 8/6/18.
 */
public class ShamrockTracer implements Tracer {

    static AtomicReference<Tracer> REF = new AtomicReference<>();

    public ShamrockTracer() {

    }

    @Override
    public String toString() {
        return "jaeger tracer";
    }

    Tracer tracer() {
        return REF.updateAndGet((orig) -> {
            if (orig != null) {
                return orig;
            }
            String serviceName = System.getProperty(JAEGER_SERVICE_NAME, System.getenv(JAEGER_SERVICE_NAME));

            if (serviceName == null) {
                return NoopTracerFactory.create();
            }

            Configuration.ReporterConfiguration reporter = new Configuration.ReporterConfiguration(
                    new UdpSender(
                            UdpSender.DEFAULT_AGENT_UDP_HOST,
                            UdpSender.DEFAULT_AGENT_UDP_COMPACT_PORT,
                            0
                    )
            );

            Configuration config = new Configuration(
                    serviceName,
                    Configuration.SamplerConfiguration.fromEnv(),
                    reporter
            );

            com.uber.jaeger.Tracer t = config.getTracerBuilder().build();
            return t;
            //System.err.println( "config: " + config );
            //Builder builder = config.getTracerBuilder();
            //StatsReporter reporter = new InMemoryStatsReporter();
            //StatsFactory statsFactory = new StatsFactoryImpl(reporter);
            //Metrics metrics = new Metrics(statsFactory);
            //metrics.baggageRestrictionsUpdateFailure = new Counter() {
            //@Override
            //public void inc(long delta) {
            //
            //}
            //}
            //builder.withMetrics(metrics );
            //System.err.println( "builder: " + builder );
            //Tracer tracer = builder.build();
            //System.err.println( "tracer: " + tracer);
            //return tracer;
        });
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
    public ActiveSpan activeSpan() {
        return tracer().activeSpan();
    }

    @Override
    public ActiveSpan makeActive(Span span) {
        return tracer().makeActive(span);
    }
}

