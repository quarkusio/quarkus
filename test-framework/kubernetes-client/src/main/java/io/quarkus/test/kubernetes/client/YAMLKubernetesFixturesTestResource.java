package io.quarkus.test.kubernetes.client;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.annotation.Annotation;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.quarkus.test.common.QuarkusTestResourceConfigurableLifecycleManager;

public class YAMLKubernetesFixturesTestResource extends AbstractKubernetesFixturesTestResource
        implements QuarkusTestResourceConfigurableLifecycleManager<WithKubernetesResourcesFromYAML> {

    private final static Logger logger = LoggerFactory.getLogger(
            YAMLKubernetesFixturesTestResource.class);

    @Override
    public void init(WithKubernetesResourcesFromYAML annotation) {
        final LinkedList<HasMetadata> resourceFixtures;
        final var yamlFiles = annotation.yamlFiles();
        if (yamlFiles != null) {
            resourceFixtures = new LinkedList<>();
            for (String yamlFile : yamlFiles) {
                try {
                    resourceFixtures.addAll(client().load(new FileInputStream(yamlFile)).get());
                } catch (FileNotFoundException e) {
                    logger().error("Couldn't find YAML file: {}", yamlFile);
                    throw new RuntimeException(e);
                }
            }

            initFixturesWithOptions(resourceFixtures,
                    annotation.readinessTimeoutSeconds(),
                    annotation.namespace(),
                    annotation.createNamespaceIfNeeded(),
                    annotation.preserveNamespaceOnError(),
                    annotation.secondsToWaitForNamespaceDeletion());
        }
    }

    @Override
    protected Class<? extends Annotation> relatedAnnotationClass() {
        return WithKubernetesResourcesFromYAML.class;
    }

    @Override
    protected Logger logger() {
        return logger;
    }
}
