package io.quarkus.hibernate.panache.deployment;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Entity;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.hibernate.panache.PanacheEntity;
import io.quarkus.hibernate.panache.managed.blocking.PanacheManagedBlockingEntity;
import io.quarkus.hibernate.panache.managed.reactive.PanacheManagedReactiveEntity;
import io.quarkus.hibernate.panache.managed.reactive.PanacheManagedReactiveRepositoryBase;
import io.quarkus.hibernate.panache.stateless.reactive.PanacheStatelessReactiveEntity;
import io.quarkus.hibernate.panache.stateless.reactive.PanacheStatelessReactiveRepositoryBase;

final class ReactivePanacheValidator {

    private static final DotName DOTNAME_ENTITY = DotName.createSimple(Entity.class.getName());

    private static final DotName DOTNAME_PANACHE_MANAGED_REACTIVE_ENTITY = DotName
            .createSimple(PanacheManagedReactiveEntity.class.getName());
    private static final DotName DOTNAME_PANACHE_STATELESS_REACTIVE_ENTITY = DotName
            .createSimple(PanacheStatelessReactiveEntity.class.getName());
    private static final DotName DOTNAME_PANACHE_MANAGED_REACTIVE_REPOSITORY_BASE = DotName
            .createSimple(PanacheManagedReactiveRepositoryBase.class.getName());
    private static final DotName DOTNAME_PANACHE_STATELESS_REACTIVE_REPOSITORY_BASE = DotName
            .createSimple(PanacheStatelessReactiveRepositoryBase.class.getName());

    private static final String PANACHE_FRAMEWORK_PACKAGE = "io.quarkus.hibernate.panache";

    static final String REACTIVE_PANACHE_REQUIRES_HIBERNATE_REACTIVE = "Reactive Panache types require the Hibernate Reactive extension. "
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
            indexer.indexClass(PanacheManagedBlockingEntity.class);
            indexer.indexClass(PanacheManagedReactiveEntity.class);
            indexer.indexClass(PanacheStatelessReactiveEntity.class);
            indexer.indexClass(PanacheManagedReactiveRepositoryBase.class);
            indexer.indexClass(PanacheStatelessReactiveRepositoryBase.class);
            indexer.indexClass(PanacheEntity.class);
            indexer.indexClass(PanacheEntity.Reactive.class);
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
