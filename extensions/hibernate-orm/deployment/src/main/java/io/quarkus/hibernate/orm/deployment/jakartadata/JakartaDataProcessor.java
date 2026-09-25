package io.quarkus.hibernate.orm.deployment.jakartadata;

import static io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME;
import static io.quarkus.security.spi.SecuredInterfaceAnnotationBuildItem.ofClassAnnotation;
import static io.quarkus.security.spi.SecuredInterfaceAnnotationBuildItem.ofMethodAnnotation;

import java.util.HashSet;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.DotName;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.hibernate.orm.deployment.ClassNames;
import io.quarkus.hibernate.orm.deployment.HibernateOrmEnabled;
import io.quarkus.hibernate.orm.deployment.HibernateOrmProcessor;
import io.quarkus.hibernate.orm.deployment.spi.AdditionalJpaModelBuildItem;
import io.quarkus.security.spi.SecuredInterfaceAnnotationBuildItem;

@BuildSteps(onlyIf = HibernateOrmEnabled.class)
public final class JakartaDataProcessor {

    private static final String JAKARTA_DATA_REPOSITORY_ANNOTATION = "jakarta.data.repository.Repository";
    private static final Set<DotName> JAKARTA_DATA_REPOSITORY_METHOD_ANNOTATIONS = Set.of(
            DotName.createSimple("jakarta.data.repository.Query"),
            DotName.createSimple("jakarta.data.repository.Find"),
            DotName.createSimple("jakarta.data.repository.Delete"),
            DotName.createSimple("jakarta.data.repository.Insert"),
            DotName.createSimple("jakarta.data.repository.Save"),
            DotName.createSimple("jakarta.data.repository.Update"));

    @BuildStep
    void registerJakartaDataRepositorySecurityAnnotations(Capabilities capabilities,
            BuildProducer<SecuredInterfaceAnnotationBuildItem> securedInterfaceAnnotationProducer) {
        if (capabilities.isPresent(Capability.SECURITY)) {
            securedInterfaceAnnotationProducer.produce(ofClassAnnotation(JAKARTA_DATA_REPOSITORY_ANNOTATION));
            HibernateOrmProcessor.HIBERNATE_REPOSITORY_ANNOTATIONS
                    .forEach(annotation -> securedInterfaceAnnotationProducer.produce(ofMethodAnnotation(annotation)));
        }
    }

    @BuildStep
    void addJakartaDataRepositoriesToJpaModel(CombinedIndexBuildItem index,
            BuildProducer<AdditionalJpaModelBuildItem> additionalJpaModel) {
        DotName repositoryAnnotation = DotName.createSimple(JAKARTA_DATA_REPOSITORY_ANNOTATION);
        Set<DotName> repositoryNames = new HashSet<>();
        for (AnnotationInstance annotation : index.getIndex().getAnnotations(repositoryAnnotation)) {
            if (annotation.target().kind() == Kind.CLASS && annotation.target().asClass().isInterface()) {
                repositoryNames.add(annotation.target().asClass().name());
            }
        }

        // Hibernate ORM 8 registers static queries only for interfaces explicitly listed in the persistence unit.
        for (DotName methodAnnotation : JAKARTA_DATA_REPOSITORY_METHOD_ANNOTATIONS) {
            for (AnnotationInstance annotation : index.getIndex().getAnnotations(methodAnnotation)) {
                if (annotation.target().kind() == Kind.METHOD
                        && annotation.target().asMethod().declaringClass().isInterface()) {
                    repositoryNames.add(annotation.target().asMethod().declaringClass().name());
                }
            }
        }

        for (DotName repositoryName : repositoryNames) {
            var repository = index.getIndex().getClassByName(repositoryName);
            var explicitAnnotation = repository.declaredAnnotation(repositoryAnnotation);
            Set<String> persistenceUnits = new HashSet<>();
            if (explicitAnnotation != null) {
                persistenceUnits.add(persistenceUnitName(explicitAnnotation));
            } else {
                // An unannotated superinterface inherits the PU of its annotated repository subinterfaces.
                for (var subinterface : index.getIndex().getAllKnownSubinterfaces(repositoryName)) {
                    var subinterfaceAnnotation = subinterface.declaredAnnotation(repositoryAnnotation);
                    if (subinterfaceAnnotation != null) {
                        persistenceUnits.add(persistenceUnitName(subinterfaceAnnotation));
                    }
                }
                if (persistenceUnits.isEmpty()) {
                    var enclosingClass = repository.enclosingClass() == null ? null
                            : index.getIndex().getClassByName(repository.enclosingClass());
                    // Nested interfaces inherit their enclosing entity's PU when the model is built.
                    if (enclosingClass == null || enclosingClass.declaredAnnotation(ClassNames.JPA_ENTITY) == null) {
                        persistenceUnits.add(DEFAULT_PERSISTENCE_UNIT_NAME);
                    }
                }
            }
            additionalJpaModel.produce(new AdditionalJpaModelBuildItem(repositoryName.toString(), persistenceUnits));
        }
    }

    private static String persistenceUnitName(AnnotationInstance repositoryAnnotation) {
        var dataStoreValue = repositoryAnnotation.value("dataStore");
        String dataStore = dataStoreValue != null ? dataStoreValue.asString() : null;
        return dataStore != null && !dataStore.isEmpty() ? dataStore : DEFAULT_PERSISTENCE_UNIT_NAME;
    }
}
