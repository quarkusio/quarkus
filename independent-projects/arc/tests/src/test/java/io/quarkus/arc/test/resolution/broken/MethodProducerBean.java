package io.quarkus.arc.test.resolution.broken;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;

@Dependent
public class MethodProducerBean {

    @Produces
    @Typed(value = MyOtherBean.class)
    public ProducedBean produceBean() {
        return new ProducedBean();
    }
}
