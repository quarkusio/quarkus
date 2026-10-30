package io.quarkus.hibernate.orm.deployment;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.hibernate.accessor.deployment.HibernateAccessorFactoryBuildItem;
import io.quarkus.hibernate.accessor.deployment.HibernateAccessorRegistrationBuildItem;
import io.quarkus.hibernate.accessor.runtime.FieldAccess;
import io.quarkus.hibernate.orm.deployment.model.JpaModelIndexBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.AdditionalJpaModelBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.HibernateOrmIntegrationStaticConfiguredBuildItem;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRecorder;

@BuildSteps(onlyIf = HibernateOrmEnabled.class)
class PropertyAccessorProcessor {

    @SuppressWarnings("deprecation")
    @BuildStep
    void registerAccessors(
            JpaModelBuildItem jpaModel,
            JpaModelIndexBuildItem indexBuildItem,
            List<AdditionalJpaModelBuildItem> additionalJpaModelBuildItems,
            List<io.quarkus.hibernate.orm.deployment.AdditionalJpaModelBuildItem> deprecatedAdditionalJpaModelBuildItems,
            BuildProducer<HibernateAccessorRegistrationBuildItem> accessorBuildItemProducer) {
        Set<String> managedClassesOnly = new HashSet<>(jpaModel.getManagedClassNames());

        // Additional model contributions guarantee indexing, but may not be discovered as managed classes.
        // Include unannotated types whose mappings are contributed at static init, e.g. Envers revision entities
        // and Hibernate Search outbox-polling entities, from both the current and deprecated build items.
        for (AdditionalJpaModelBuildItem additionalJpaModelBuildItem : additionalJpaModelBuildItems) {
            managedClassesOnly.add(additionalJpaModelBuildItem.getClassName());
        }
        for (io.quarkus.hibernate.orm.deployment.AdditionalJpaModelBuildItem additionalJpaModelBuildItem : deprecatedAdditionalJpaModelBuildItems) {
            managedClassesOnly.add(additionalJpaModelBuildItem.getClassName());
        }

        for (String managedClassName : managedClassesOnly) {
            ClassInfo classToAccess = indexBuildItem.getIndex().getClassByName(managedClassName);
            HibernateAccessorRegistrationBuildItem.Builder builder = new HibernateAccessorRegistrationBuildItem.Builder(
                    classToAccess)
                    .source(PropertyAccessorProcessor.class.getName())
                    .all(classToAccess, FieldAccess.READ);
            // Record components are never written, whatever the strategy.
            if (!classToAccess.isRecord()) {
                for (FieldInfo field : classToAccess.fields()) {
                    if (isPersistentCandidate(field)) {
                        builder.addFieldWriter(field);
                    }
                }
            }
            accessorBuildItemProducer.produce(builder.build());
        }
    }

    /**
     * Tells whether a field may hold a persistent attribute, using only what JPA guarantees is never
     * mapped. Hibernate's own metadata is not built yet at this point, so this cannot be exact; it only
     * has to be conservative. Registering a writer makes generated strategies drop {@code final} from the
     * field, so fields JPA always ignores are kept out to leave their modifiers untouched.
     */
    private static boolean isPersistentCandidate(FieldInfo field) {
        return !Modifier.isStatic(field.flags())
                && !Modifier.isTransient(field.flags())
                && !field.hasAnnotation(ClassNames.TRANSIENT);
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void configurePropertyAccessors(HibernateOrmRecorder recorder,
            HibernateAccessorFactoryBuildItem hibernateAccessorFactory,
            List<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptors,
            BuildProducer<HibernateOrmIntegrationStaticConfiguredBuildItem> integrations) {
        for (PersistenceUnitDescriptorBuildItem persistenceUnit : persistenceUnitDescriptors) {
            integrations.produce(HibernateOrmIntegrationStaticConfiguredBuildItem
                    .builder("Hibernate Accessor", persistenceUnit.getPersistenceUnitName())
                    .initListener(recorder.propertyAccessorIntegration(hibernateAccessorFactory.accessorFactory()))
                    .build());
        }
    }
}
