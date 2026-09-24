package io.quarkus.hibernate.orm.deployment.jakartadata;

import static io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME;
import static io.quarkus.security.spi.SecuredInterfaceAnnotationBuildItem.ofClassAnnotation;
import static io.quarkus.security.spi.SecuredInterfaceAnnotationBuildItem.ofMethodAnnotation;

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
import io.quarkus.hibernate.orm.deployment.HibernateOrmEnabled;
import io.quarkus.hibernate.orm.deployment.HibernateOrmProcessor;
import io.quarkus.hibernate.orm.deployment.spi.AdditionalJpaModelBuildItem;
import io.quarkus.security.spi.SecuredInterfaceAnnotationBuildItem;

@BuildSteps(onlyIf = HibernateOrmEnabled.class)
public final class JakartaDataProcessor {

    private static final String JAKARTA_DATA_REPOSITORY_ANNOTATION = "jakarta.data.repository.Repository";

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
        for (AnnotationInstance annotation : index.getIndex().getAnnotations(repositoryAnnotation)) {
            if (annotation.target().kind() != Kind.CLASS) {
                continue;
            }
            String className = annotation.target().asClass().name().toString();
            var dataStoreValue = annotation.value("dataStore");
            String dataStore = dataStoreValue != null ? dataStoreValue.asString() : null;
            if (dataStore != null && !dataStore.isEmpty()) {
                additionalJpaModel.produce(new AdditionalJpaModelBuildItem(className, Set.of(dataStore)));
            } else {
                additionalJpaModel.produce(new AdditionalJpaModelBuildItem(className, Set.of(DEFAULT_PERSISTENCE_UNIT_NAME)));
            }
        }
    }
}
