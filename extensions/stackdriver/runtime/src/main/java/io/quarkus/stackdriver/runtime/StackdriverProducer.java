package io.quarkus.stackdriver.runtime;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;

@Dependent
public class StackdriverProducer {

    public StackdriverProducer() {

    }

    @Produces
    public SpanService spanService() {
        return new SpanService();
    }
}
