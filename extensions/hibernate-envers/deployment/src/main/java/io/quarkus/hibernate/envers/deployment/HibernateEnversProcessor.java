package io.quarkus.hibernate.envers.deployment;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.NativeImageFeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.hibernate.accessor.deployment.HibernateAccessorBuildItem;
import io.quarkus.hibernate.envers.HibernateEnversBuildTimeConfig;
import io.quarkus.hibernate.envers.HibernateEnversBuildTimeConfigPersistenceUnit;
import io.quarkus.hibernate.envers.HibernateEnversRecorder;
import io.quarkus.hibernate.envers.runtime.graal.DisableLoggingFeature;
import io.quarkus.hibernate.orm.deployment.PersistenceUnitDescriptorBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.AdditionalJpaModelBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.HibernateOrmIntegrationStaticConfiguredBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.component.PersistenceUnitRequestBuildItem;
import io.quarkus.runtime.util.ProgrammingParadigm;

@BuildSteps(onlyIf = HibernateEnversEnabled.class)
public final class HibernateEnversProcessor {

    public static final String[] ENVERS_CLASSES_FOR_REFLECTION = new String[] {
            "org.hibernate.envers.DefaultRevisionEntity",
            "org.hibernate.envers.DefaultTrackingModifiedEntitiesRevisionEntity",
            "org.hibernate.envers.RevisionMapping",
            "org.hibernate.envers.TrackingModifiedEntitiesRevisionMapping",
            "org.hibernate.envers.enhanced.SequenceIdRevisionEntity",
            "org.hibernate.envers.enhanced.SequenceIdTrackingModifiedEntitiesRevisionEntity",
            "org.hibernate.envers.enhanced.SequenceIdRevisionMapping",
            "org.hibernate.envers.enhanced.SequenceIdTrackingModifiedEntitiesRevisionMapping"
    };
    static final String HIBERNATE_ENVERS = "Hibernate Envers";

    @BuildStep
    void collectImplicitPersistenceUnitRequests(HibernateEnversBuildTimeConfig config,
            BuildProducer<PersistenceUnitRequestBuildItem> puRequests) {
        for (String name : config.persistenceUnits().keySet()) {
            puRequests.produce(new PersistenceUnitRequestBuildItem(name, ProgrammingParadigm.BLOCKING,
                    String.format("Configuration '%s'", HibernateEnversBuildTimeConfig.persistenceUnitPropertyKey(name, "*"))));
        }
    }

    @BuildStep
    void addJpaModelClasses(BuildProducer<AdditionalJpaModelBuildItem> producer) {
        // These are added to specific PUs at static init using org.hibernate.boot.spi.AdditionalMappingContributor,
        // so we pass empty sets of PUs.
        // The build items tell the Hibernate extension to process the classes at build time:
        // add to Jandex index, bytecode enhancement, proxy generation, ...
        // Include both native-id and sequence-id revision entities and the mapped superclasses declaring their fields,
        // so ORM can register accessors before Envers contributes its mappings at static init.
        for (String klass : ENVERS_CLASSES_FOR_REFLECTION) {
            producer.produce(new AdditionalJpaModelBuildItem(klass, Set.of()));
        }
    }

    @BuildStep
    public void registerEnversReflections(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<HibernateAccessorBuildItem> accessorBuildItemBuildProducer,
            HibernateEnversBuildTimeConfig buildTimeConfig) {
        // This is necessary because these classes are added to the model conditionally at static init,
        // so they don't get processed by HibernateOrmProcessor and in particular don't get reflection enabled.
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(
                ENVERS_CLASSES_FOR_REFLECTION)
                .reason(getClass().getName())
                .methods().build());
        for (String klass : ENVERS_CLASSES_FOR_REFLECTION) {
            accessorBuildItemBuildProducer.produce(new HibernateAccessorBuildItem.Builder(
                    klass.substring(0, klass.lastIndexOf('.')), klass, klass, true, false, false).addDefaultConstructor()
                    .build());
        }

        List<String> classes = new ArrayList<>(buildTimeConfig.persistenceUnits().size() * 2);
        for (HibernateEnversBuildTimeConfigPersistenceUnit pu : buildTimeConfig.persistenceUnits().values()) {
            pu.revisionListener().ifPresent(classes::add);
            pu.auditStrategy().ifPresent(classes::add);
        }
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(classes)
                .reason("Configured Envers listeners and audit strategies")
                .methods().fields().build());
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    NativeImageFeatureBuildItem nativeImageFeature() {
        return new NativeImageFeatureBuildItem(DisableLoggingFeature.class);
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void applyStaticConfig(HibernateEnversRecorder recorder, HibernateEnversBuildTimeConfig buildTimeConfig,
            List<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptorBuildItems,
            BuildProducer<HibernateOrmIntegrationStaticConfiguredBuildItem> integrationProducer) {
        for (PersistenceUnitDescriptorBuildItem puDescriptor : persistenceUnitDescriptorBuildItems) {
            String puName = puDescriptor.getPersistenceUnitName();
            integrationProducer.produce(
                    HibernateOrmIntegrationStaticConfiguredBuildItem.builder(HIBERNATE_ENVERS, puName)
                            .initListener(recorder.createStaticInitListener(buildTimeConfig, puName))
                            .xmlMappingRequired(true)
                            .build());
        }
    }
}
