package io.quarkus.spring.data.rest.deployment;

import static io.quarkus.spring.data.rest.deployment.RepositoryMethodsImplementor.CRUD_REPOSITORY_INTERFACE;
import static io.quarkus.spring.data.rest.deployment.RepositoryMethodsImplementor.JPA_REPOSITORY_INTERFACE;
import static io.quarkus.spring.data.rest.deployment.RepositoryMethodsImplementor.LIST_CRUD_REPOSITORY_INTERFACE;
import static io.quarkus.spring.data.rest.deployment.RepositoryMethodsImplementor.LIST_PAGING_AND_SORTING_REPOSITORY_INTERFACE;
import static io.quarkus.spring.data.rest.deployment.RepositoryMethodsImplementor.PAGING_AND_SORTING_REPOSITORY_INTERFACE;
import static java.lang.constant.ConstantDescs.CD_void;

import java.lang.constant.ClassDesc;
import java.util.List;

import jakarta.persistence.Id;

import org.hibernate.bytecode.enhance.spi.EnhancerConstants;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.gizmo2.Jandex2Gizmo;

import io.quarkus.deployment.bean.JavaBeanUtil;
import io.quarkus.gizmo2.desc.ClassMethodDesc;
import io.quarkus.gizmo2.desc.MethodDesc;

public class EntityClassHelper {

    private final IndexView index;

    public EntityClassHelper(IndexView index) {
        this.index = index;
    }

    public FieldInfo getIdField(String className) {
        return getIdField(index.getClassByName(DotName.createSimple(className)));
    }

    public FieldInfo getIdField(ClassInfo classInfo) {
        ClassInfo tmpClassInfo = classInfo;
        while (tmpClassInfo != null) {
            for (FieldInfo field : tmpClassInfo.fields()) {
                if (field.hasAnnotation(DotName.createSimple(Id.class.getName()))) {
                    return field;
                }
            }
            if (tmpClassInfo.superName() != null) {
                tmpClassInfo = index.getClassByName(tmpClassInfo.superName());
            } else {
                tmpClassInfo = null;
            }
        }
        throw new IllegalArgumentException("Couldn't find id field of " + classInfo);
    }

    public MethodDesc getSetter(String className, FieldInfo field) {
        return getSetter(index.getClassByName(DotName.createSimple(className)), field);
    }

    public MethodDesc getSetter(ClassInfo entityClass, FieldInfo field) {
        MethodDesc setter = getMethod(entityClass, JavaBeanUtil.getSetterName(field.name()), field.type());
        if (setter != null) {
            return setter;
        }
        return ClassMethodDesc.of(
                ClassDesc.of(entityClass.toString()),
                EnhancerConstants.PERSISTENT_FIELD_WRITER_PREFIX + field.name(),
                CD_void,
                Jandex2Gizmo.classDescOf(field.type()));
    }

    public MethodDesc getMethod(ClassInfo entityClass, String name, Type... parameters) {
        if (entityClass == null) {
            return null;
        }
        MethodInfo methodInfo = entityClass.method(name, parameters);
        if (methodInfo != null) {
            return Jandex2Gizmo.methodDescOf(methodInfo);
        } else if (entityClass.superName() != null) {
            return getMethod(index.getClassByName(entityClass.superName()), name, parameters);
        }
        return null;
    }

    public boolean isRepositoryInstanceOf(DotName target, String repositoryName) {
        ClassInfo classByName = index.getClassByName(repositoryName);
        List<Type> types = classByName.interfaceTypes();
        return types.stream().anyMatch(type -> type.name().equals(target));
    }

    public boolean isCrudRepository(String repositoryName) {
        return isRepositoryInstanceOf(CRUD_REPOSITORY_INTERFACE, repositoryName)
                || isRepositoryInstanceOf(LIST_CRUD_REPOSITORY_INTERFACE, repositoryName)
                || isRepositoryInstanceOf(JPA_REPOSITORY_INTERFACE, repositoryName);
    }

    public boolean isListCrudRepository(String repositoryName) {
        return isRepositoryInstanceOf(LIST_CRUD_REPOSITORY_INTERFACE, repositoryName)
                || isRepositoryInstanceOf(JPA_REPOSITORY_INTERFACE, repositoryName);
    }

    public boolean isPagingAndSortingRepository(String repositoryName) {
        return isRepositoryInstanceOf(PAGING_AND_SORTING_REPOSITORY_INTERFACE, repositoryName)
                || isRepositoryInstanceOf(LIST_PAGING_AND_SORTING_REPOSITORY_INTERFACE, repositoryName)
                || isRepositoryInstanceOf(JPA_REPOSITORY_INTERFACE, repositoryName);
    }

    public boolean isListPagingAndSortingRepository(String repositoryName) {
        return isRepositoryInstanceOf(LIST_PAGING_AND_SORTING_REPOSITORY_INTERFACE, repositoryName)
                || isRepositoryInstanceOf(JPA_REPOSITORY_INTERFACE, repositoryName);
    }

    public boolean containsPagedRepository(List<ClassInfo> repositories) {
        return repositories.stream().anyMatch(r -> isPagingAndSortingRepository(r.name().toString()));
    }
}
