package io.quarkus.arc.test.resolution.broken;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Typed;

@Dependent
@Typed(value = MyOtherBean.class)
public class MyBean {
}
