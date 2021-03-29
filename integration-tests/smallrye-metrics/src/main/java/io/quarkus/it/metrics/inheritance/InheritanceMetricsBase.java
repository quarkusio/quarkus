package io.quarkus.it.metrics.inheritance;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.metrics.annotation.Counted;

@Counted
@ApplicationScoped
public class InheritanceMetricsBase {

    public void baseMethod() {

    }

}
