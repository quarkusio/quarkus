package org.acme.it.tck.deployment;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import org.jboss.jandex.AnnotationTransformation;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;

public class TCKProcessor {
    @BuildStep
    public void tck(
            BuildProducer<IndexDependencyBuildItem> indexDependency) {
        indexDependency.produce(new IndexDependencyBuildItem("org.acme", "tck-runner"));
    }

    /**
     * Discover and registers the TCK test classes as CDI Beans. Quarkus requires test classes to be CDI Beans, which
     * are created automatically when a test class is annotated with @QuarkusTest. To avoid overriding each test class
     * to add the annotation, we register the QuarkusTextExtension globally via JUnit ServiceLoader and create a bean
     * for each test class.
     */
    @BuildStep
    public void testBeans(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformer) {

        annotationsTransformer.produce(new AnnotationsTransformerBuildItem(
                AnnotationTransformation.forClasses()
                        .whenClass(c -> "org.acme.tck.HelloResourceTest".equals(c.name().toString()))
                        .transform(ctx -> ctx.add(ApplicationScoped.class))));
    }
}
