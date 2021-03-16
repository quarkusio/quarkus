package io.quarkus.it.opentracing;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.opentracing.Traced;

@ApplicationScoped
public class TracedService {
    @Traced
    public String testTraced() {
        return "TEST";
    }
}
