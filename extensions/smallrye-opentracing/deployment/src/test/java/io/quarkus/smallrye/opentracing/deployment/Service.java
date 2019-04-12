package io.quarkus.smallrye.opentracing.deployment;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.opentracing.Traced;

@Traced
@ApplicationScoped
public class Service {

    public void foo() {
    }
}
