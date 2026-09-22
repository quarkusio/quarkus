package io.quarkus.hibernate.orm.deployment.jakartadata;

import static io.quarkus.security.spi.SecuredInterfaceAnnotationBuildItem.ofClassAnnotation;
import static io.quarkus.security.spi.SecuredInterfaceAnnotationBuildItem.ofMethodAnnotation;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.hibernate.orm.deployment.HibernateOrmEnabled;
import io.quarkus.hibernate.orm.deployment.HibernateOrmProcessor;
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
}
