package org.jboss.shamrock.opentracing.runtime;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
/**
 * Created by bob on 8/6/18.
 */
@Dependent
public class TracerProducer {
    @Produces
    @Dependent
    Tracer tracer(InjectionPoint ip) {
        Tracer tracer = GlobalTracer.get();
        System.err.println( "producing tracer: " + tracer + " for " + ip);
        return tracer;
    }
}
