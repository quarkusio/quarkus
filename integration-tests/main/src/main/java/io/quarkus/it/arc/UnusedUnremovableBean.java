package io.quarkus.it.arc;

import jakarta.enterprise.context.Dependent;

import io.quarkus.arc.Unremovable;

@Dependent
@Unremovable
public class UnusedUnremovableBean {
}
