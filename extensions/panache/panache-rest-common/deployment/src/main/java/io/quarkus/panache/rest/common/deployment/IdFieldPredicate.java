package io.quarkus.panache.rest.common.deployment;

import java.util.function.Predicate;

import org.jboss.jandex.FieldInfo;

public interface IdFieldPredicate extends Predicate<FieldInfo> {
}
