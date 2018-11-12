package org.jboss.shamrock.opentracing.runtime;

import java.util.concurrent.atomic.AtomicReference;

import com.uber.jaeger.Configuration;
import io.opentracing.ActiveSpan;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;

import static com.uber.jaeger.Configuration.JAEGER_SERVICE_NAME;

/**
 * Created by bob on 8/6/18.
 */
public class ShamrockTracer implements Tracer {

    AtomicReference<Tracer> ref = new AtomicReference<>();

    Tracer tracer() {
        return this.ref.updateAndGet( (orig)->{
            if ( orig != null ) {
                return orig;
            }
            //return Configuration.fromEnv().getTracer();
            //Configuration config = Configuration.fromEnv();
            //Builder builder = config.getTracerBuilder();
            //Configuration.ReporterConfiguration reporterFromEnv = Configuration.ReporterConfiguration.fromEnv();
            //Configuration.ReporterConfiguration reporter = new Configuration.ReporterConfiguration(new DamnitSender());

            Configuration config = new Configuration(
                    System.getProperty(JAEGER_SERVICE_NAME, System.getenv(JAEGER_SERVICE_NAME)));

            com.uber.jaeger.Tracer t = config.getTracerBuilder().build();
            System.err.println( "init tracer: " + t);
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
        System.err.println( "** build span: " + operationName);
        new Exception().printStackTrace();
        return tracer().buildSpan(operationName);
    }

    @Override
    public <C> void inject(SpanContext spanContext, Format<C> format, C carrier) {
        System.err.println( "** inject");
        tracer().inject(spanContext, format, carrier);
    }

    @Override
    public <C> SpanContext extract(Format<C> format, C carrier) {
        System.err.println( "** extract");
        return tracer().extract(format, carrier);
    }

    @Override
    public ActiveSpan activeSpan() {
        System.err.println( "** activeSpan");
        return tracer().activeSpan();
    }

    @Override
    public ActiveSpan makeActive(Span span) {
        System.err.println( "** makeActive");
        return tracer().makeActive(span);
    }
}

