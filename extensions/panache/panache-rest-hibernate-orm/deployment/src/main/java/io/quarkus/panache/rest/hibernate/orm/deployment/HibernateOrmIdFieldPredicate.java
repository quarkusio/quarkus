package io.quarkus.panache.rest.hibernate.orm.deployment;

import javax.persistence.Id;

import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;

import io.quarkus.panache.rest.common.deployment.IdFieldPredicate;

public class HibernateOrmIdFieldPredicate implements IdFieldPredicate {

    @Override
    public boolean test(FieldInfo fieldInfo) {
        return fieldInfo.hasAnnotation(DotName.createSimple(Id.class.getName()));
    }
}
