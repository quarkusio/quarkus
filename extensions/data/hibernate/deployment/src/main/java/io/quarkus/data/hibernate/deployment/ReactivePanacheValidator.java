package io.quarkus.data.hibernate.deployment;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Entity;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.data.hibernate.managed.reactive.ReactiveManagedEntity;
import io.quarkus.data.hibernate.managed.reactive.ReactiveManagedRepositoryBase;
import io.quarkus.data.hibernate.stateless.reactive.ReactiveRecordEntity;
import io.quarkus.data.hibernate.stateless.reactive.ReactiveRecordRepositoryBase;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;

final class ReactivePanacheValidator {

    private static final DotName DOTNAME_ENTITY = DotName.createSimple(Entity.class.getName());

    private static final DotName DOTNAME_REACTIVE_MANAGED_ENTITY = DotName
            .createSimple(ReactiveManagedEntity.class.getName());
    private static final DotName DOTNAME_REACTIVE_RECORD_ENTITY = DotName
            .createSimple(ReactiveRecordEntity.class.getName());
    private static final DotName DOTNAME_REACTIVE_MANAGED_REPOSITORY_BASE = DotName
            .createSimple(ReactiveManagedRepositoryBase.class.getName());
    private static final DotName DOTNAME_REACTIVE_RECORD_REPOSITORY_BASE = DotName
            .createSimple(ReactiveRecordRepositoryBase.class.getName());

    private static final String DATA_HIBERNATE_FRAMEWORK_PACKAGE = "io.quarkus.data.hibernate";

    static final String REACTIVE_PANACHE_REQUIRES_HIBERNATE_REACTIVE = "Reactive Quarkus Data types require the Hibernate Reactive extension. "
            + "Add the 'quarkus-hibernate-reactive' extension and a reactive driver extension "
            + "(for example 'quarkus-reactive-pg-client') to your project dependencies.";

    private ReactivePanacheValidator() {
    }

    static Set<String> findOffendingReactivePanacheTypes(IndexView index, Capabilities capabilities) {
        if (capabilities.isPresent(Capability.HIBERNATE_REACTIVE)) {
            return Collections.emptySet();
        }

        Set<String> offendingTypes = new LinkedHashSet<>();

        for (ClassInfo classInfo : index.getAllKnownImplementations(DOTNAME_REACTIVE_MANAGED_ENTITY)) {
            if (isReactiveFrameworkEntityType(classInfo.name())) {
                continue;
            }
            if (classInfo.declaredAnnotation(DOTNAME_ENTITY) != null) {
                offendingTypes.add(classInfo.name().toString());
            }
        }

        for (ClassInfo classInfo : index.getAllKnownImplementations(DOTNAME_REACTIVE_RECORD_ENTITY)) {
            if (isReactiveFrameworkEntityType(classInfo.name())) {
                continue;
            }
            if (classInfo.declaredAnnotation(DOTNAME_ENTITY) != null) {
                offendingTypes.add(classInfo.name().toString());
            }
        }

        for (ClassInfo classInfo : index.getAllKnownSubinterfaces(DOTNAME_REACTIVE_MANAGED_REPOSITORY_BASE)) {
            if (classInfo.isInterface() && isUserDefinedReactiveType(classInfo)) {
                offendingTypes.add(classInfo.name().toString());
            }
        }

        for (ClassInfo classInfo : index.getAllKnownSubinterfaces(DOTNAME_REACTIVE_RECORD_REPOSITORY_BASE)) {
            if (classInfo.isInterface() && isUserDefinedReactiveType(classInfo)) {
                offendingTypes.add(classInfo.name().toString());
            }
        }

        return offendingTypes;
    }

    private static boolean isReactiveFrameworkEntityType(DotName name) {
        return name.equals(DOTNAME_REACTIVE_MANAGED_ENTITY)
                || name.equals(DOTNAME_REACTIVE_RECORD_ENTITY);
    }

    private static boolean isUserDefinedReactiveType(ClassInfo classInfo) {
        return !classInfo.name().toString().startsWith(DATA_HIBERNATE_FRAMEWORK_PACKAGE);
    }
}
