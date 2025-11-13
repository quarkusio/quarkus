package io.quarkus.hibernate.reactive.transactions.deployment;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.runtime.configuration.ConfigurationException;

class HibernateReactiveTransactionsProcessor {

    private static final String FEATURE = "quarkus-reactive-transactions";

    private static final DotName TRANSACTIONAL = DotName.createSimple(Transactional.class.getName());

    private static final String WITH_SESSION_ON_DEMAND = "io.quarkus.hibernate.reactive.panache.common.WithSessionOnDemand";
    private static final DotName WITH_SESSION = DotName
            .createSimple("io.quarkus.hibernate.reactive.panache.common.WithSession");
    private static final DotName WITH_TRANSACTION = DotName
            .createSimple("io.quarkus.hibernate.reactive.panache.common.WithTransaction");

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @Inject
    CombinedIndexBuildItem combinedIndexBuildItem;

    @BuildStep
    void register(
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass // TOOD Luca hack to make this build step run
    ) {

        IndexView index = combinedIndexBuildItem.getIndex();

        for (AnnotationInstance deserializeInstance : index.getAnnotations(TRANSACTIONAL)) {
            AnnotationTarget annotationTarget = deserializeInstance.target();

            if (annotationTarget.hasAnnotation(WITH_SESSION_ON_DEMAND)) {
                throw new ConfigurationException("Cannot mix @Transactional and @WithSessionOnDemand");
            }

            if (annotationTarget.hasAnnotation(WITH_SESSION)) {
                throw new ConfigurationException("Cannot mix @Transactional and @WithSession");
            }

            if (annotationTarget.hasAnnotation(WITH_TRANSACTION)) {
                throw new ConfigurationException("Cannot mix @Transactional and @WithTransaction");
            }

        }

    }
}
