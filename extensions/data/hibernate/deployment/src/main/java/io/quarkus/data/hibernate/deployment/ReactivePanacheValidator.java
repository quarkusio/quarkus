package io.quarkus.data.hibernate.deployment;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Entity;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;

import io.quarkus.data.hibernate.ManagedEntity;
import io.quarkus.data.hibernate.managed.blocking.BlockingManagedEntity;
import io.quarkus.data.hibernate.managed.reactive.ReactiveManagedEntity;
import io.quarkus.data.hibernate.managed.reactive.ReactiveManagedRepositoryBase;
import io.quarkus.data.hibernate.stateless.reactive.ReactiveRecordEntity;
import io.quarkus.data.hibernate.stateless.reactive.ReactiveRecordRepositoryBase;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;

final class ReactivePanacheValidator {

    private static final DotName DOTNAME_ENTITY = DotName.createSimple(Entity.class.getName());

    private static final DotName DOTNAME_PANACHE_MANAGED_REACTIVE_ENTITY = DotName
            .createSimple(ReactiveManagedEntity.class.getName());
    private static final DotName DOTNAME_PANACHE_STATELESS_REACTIVE_ENTITY = DotName
            .createSimple(ReactiveRecordEntity.class.getName());
    private static final DotName DOTNAME_PANACHE_MANAGED_REACTIVE_REPOSITORY_BASE = DotName
            .createSimple(ReactiveManagedRepositoryBase.class.getName());
    private static final DotName DOTNAME_PANACHE_STATELESS_REACTIVE_REPOSITORY_BASE = DotName
            .createSimple(ReactiveRecordRepositoryBase.class.getName());

    private static final String PANACHE_FRAMEWORK_PACKAGE = "io.quarkus.data.hibernate";

    static final String REACTIVE_PANACHE_REQUIRES_HIBERNATE_REACTIVE = "Reactive Quarkus Data Hibernate types require the Hibernate Reactive extension. "
            + "Add the 'quarkus-hibernate-reactive' extension and a reactive driver extension "
            + "(for example 'quarkus-reactive-pg-client') to your project dependencies.";

    private ReactivePanacheValidator() {
    }

    static Set<String> findOffendingReactivePanacheTypes(IndexView index, Capabilities capabilities) {
        if (capabilities.isPresent(Capability.HIBERNATE_REACTIVE)) {
            return Collections.emptySet();
        }

        Set<String> offendingTypes = new LinkedHashSet<>();

        for (ClassInfo classInfo : index.getAllKnownImplementations(DOTNAME_PANACHE_MANAGED_REACTIVE_ENTITY)) {
            if (isReactivePanacheFrameworkType(classInfo.name())) {
                continue;
            }
            if (classInfo.declaredAnnotation(DOTNAME_ENTITY) != null) {
                offendingTypes.add(classInfo.name().toString());
            }
        }

        for (ClassInfo classInfo : index.getAllKnownImplementations(DOTNAME_PANACHE_STATELESS_REACTIVE_ENTITY)) {
            if (isReactivePanacheFrameworkType(classInfo.name())) {
                continue;
            }
            if (classInfo.declaredAnnotation(DOTNAME_ENTITY) != null) {
                offendingTypes.add(classInfo.name().toString());
            }
        }

        for (ClassInfo classInfo : index.getAllKnownSubinterfaces(DOTNAME_PANACHE_MANAGED_REACTIVE_REPOSITORY_BASE)) {
            if (classInfo.isInterface() && isUserDefinedReactivePanacheType(classInfo)) {
                offendingTypes.add(classInfo.name().toString());
            }
        }

        for (ClassInfo classInfo : index.getAllKnownSubinterfaces(DOTNAME_PANACHE_STATELESS_REACTIVE_REPOSITORY_BASE)) {
            if (classInfo.isInterface() && isUserDefinedReactivePanacheType(classInfo)) {
                offendingTypes.add(classInfo.name().toString());
            }
        }

        return offendingTypes;
    }

    static IndexView indexOf(Class<?>... classes) {
        try {
            Indexer indexer = new Indexer();
            indexer.indexClass(BlockingManagedEntity.class);
            indexer.indexClass(ReactiveManagedEntity.class);
            indexer.indexClass(ReactiveRecordEntity.class);
            indexer.indexClass(ReactiveManagedRepositoryBase.class);
            indexer.indexClass(ReactiveRecordRepositoryBase.class);
            indexer.indexClass(ManagedEntity.class);
            indexer.indexClass(ManagedEntity.Reactive.class);
            indexer.indexClass(Entity.class);
            for (Class<?> clazz : classes) {
                indexer.indexClass(clazz);
            }
            return indexer.complete();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isReactivePanacheFrameworkType(DotName name) {
        return name.equals(DOTNAME_PANACHE_MANAGED_REACTIVE_ENTITY)
                || name.equals(DOTNAME_PANACHE_STATELESS_REACTIVE_ENTITY);
    }

    private static boolean isUserDefinedReactivePanacheType(ClassInfo classInfo) {
        return !classInfo.name().toString().startsWith(PANACHE_FRAMEWORK_PACKAGE);
    }
}
