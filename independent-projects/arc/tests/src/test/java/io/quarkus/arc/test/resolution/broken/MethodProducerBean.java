package io.quarkus.arc.test.resolution.broken;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Typed;

@Dependent
public class MethodProducerBean {

    @Produces
    @Typed(value = MyOtherBean.class)
    public ProducedBean produceBean() {
        return new ProducedBean();
    }
}
