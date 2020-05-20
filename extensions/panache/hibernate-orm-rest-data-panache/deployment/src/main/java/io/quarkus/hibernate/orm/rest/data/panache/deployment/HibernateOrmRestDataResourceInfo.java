package io.quarkus.hibernate.orm.rest.data.panache.deployment;

import java.util.function.Predicate;

import javax.persistence.Id;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;

import io.quarkus.rest.data.panache.deployment.DataAccessImplementor;
import io.quarkus.rest.data.panache.deployment.RestDataResourceInfo;

public class HibernateOrmRestDataResourceInfo implements RestDataResourceInfo {

    private final ClassInfo classInfo;

    private final String idClassName;

    private final String entityClassName;

    private final DataAccessImplementor dataAccessImplementor;

    private HibernateOrmRestDataResourceInfo(ClassInfo classInfo, String idClassName, String entityClassName,
            DataAccessImplementor dataAccessImplementor) {
        this.classInfo = classInfo;
        this.idClassName = idClassName;
        this.entityClassName = entityClassName;
        this.dataAccessImplementor = dataAccessImplementor;
    }

    public static RestDataResourceInfo withEntityAccess(ClassInfo classInfo, String idClassName, String entityClassName) {
        return new HibernateOrmRestDataResourceInfo(classInfo, idClassName, entityClassName,
                new EntityDataAccessImplementor(entityClassName));
    }

    public static RestDataResourceInfo withRepositoryAccess(ClassInfo classInfo, String idClassName, String entityClassName,
            String repositoryClassName) {
        return new HibernateOrmRestDataResourceInfo(classInfo, idClassName, entityClassName,
                new RepositoryDataAccessImplementor(repositoryClassName));
    }

    @Override
    public ClassInfo getClassInfo() {
        return classInfo;
    }

    @Override
    public String getIdClassName() {
        return idClassName;
    }

    @Override
    public String getEntityClassName() {
        return entityClassName;
    }

    @Override
    public DataAccessImplementor getDataAccessImplementor() {
        return dataAccessImplementor;
    }

    @Override
    public Predicate<FieldInfo> getIdFieldPredicate() {
        return fieldInfo -> fieldInfo.hasAnnotation(DotName.createSimple(Id.class.getName()));
    }
}
