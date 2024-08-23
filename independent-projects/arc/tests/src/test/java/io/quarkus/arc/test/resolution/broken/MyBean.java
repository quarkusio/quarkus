package io.quarkus.arc.test.resolution.broken;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Typed;

@Dependent
@Typed(value = MyOtherBean.class)
public class MyBean {
}
