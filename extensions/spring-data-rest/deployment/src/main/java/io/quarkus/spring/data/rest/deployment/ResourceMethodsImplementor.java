package io.quarkus.spring.data.rest.deployment;

import io.quarkus.gizmo.ClassCreator;

public interface ResourceMethodsImplementor {

    void implementList(ClassCreator classCreator, String repositoryInterface);

    void implementListPageCount(ClassCreator classCreator, String repositoryInterface);

    void implementGet(ClassCreator classCreator, String repositoryInterface);

    void implementAdd(ClassCreator classCreator, String repositoryInterface);

    void implementUpdate(ClassCreator classCreator, String repositoryInterface, String entityType);

    void implementDelete(ClassCreator classCreator, String repositoryInterface);
}
