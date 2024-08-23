package io.quarkus.it.metrics.inheritance;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.metrics.annotation.Counted;

@Counted
@ApplicationScoped
public class InheritanceMetricsBase {

    public void baseMethod() {

    }

}
