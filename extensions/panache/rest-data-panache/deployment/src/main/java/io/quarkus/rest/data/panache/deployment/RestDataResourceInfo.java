package io.quarkus.rest.data.panache.deployment;

import java.util.function.Predicate;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;

public interface RestDataResourceInfo {

    ClassInfo getClassInfo();

    DataAccessImplementor getDataAccessImplementor();

    String getIdClassName();

    String getEntityClassName();

    Predicate<FieldInfo> getIdFieldPredicate();
}
