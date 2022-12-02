package io.quarkus.it.arc;

import javax.enterprise.context.Dependent;

import io.quarkus.arc.Unremovable;

@Dependent
@Unremovable
public class UnusedUnremovableBean {
}
