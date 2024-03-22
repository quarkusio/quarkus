package io.quarkus.test.kubernetes.client;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.quarkus.test.common.QuarkusTestResourceConfigurableLifecycleManager;

public class FactoryKubernetesFixturesTestResource extends AbstractKubernetesFixturesTestResource
        implements
        QuarkusTestResourceConfigurableLifecycleManager<WithKubernetesResourcesFromFactory> {
    private final static Logger logger = LoggerFactory.getLogger(
            FactoryKubernetesFixturesTestResource.class);

    @Override
    public void init(WithKubernetesResourcesFromFactory annotation) {
        final var factoryClass = annotation.factory();
        try {
            KubernetesResourcesFactory factory = factoryClass.getConstructor().newInstance();
            final var resourceFixtures = new LinkedList<HasMetadata>(factory.build(namespace()));
            initFixturesWithOptions(resourceFixtures,
                    annotation.readinessTimeoutSeconds(),
                    annotation.namespace(),
                    annotation.createNamespaceIfNeeded(),
                    annotation.preserveNamespaceOnError(),
                    annotation.secondsToWaitForNamespaceDeletion());
        } catch (InstantiationException | NoSuchMethodException | InvocationTargetException
                | IllegalAccessException e) {
            logger().error("Couldn't instantiate {} {} class", factoryClass.getSimpleName(),
                    KubernetesResourcesFactory.class.getSimpleName());
            throw new RuntimeException(e);
        }

    }

    @Override
    protected Class<? extends Annotation> relatedAnnotationClass() {
        return WithKubernetesResourcesFromFactory.class;
    }

    @Override
    protected Logger logger() {
        return logger;
    }
}
