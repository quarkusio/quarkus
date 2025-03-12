package org.acme.it.tck.deployment;

import static org.jboss.jandex.AnnotationTarget.Kind.CLASS;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.jandex.AnnotationTarget;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;

public class TCKProcessor {
    @BuildStep
    public void tck(
            BuildProducer<IndexDependencyBuildItem> indexDependency,
            BuildProducer<ResteasyJaxrsProviderBuildItem> providers) {
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

        annotationsTransformer.produce(new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
            @Override
            public boolean appliesTo(final AnnotationTarget.Kind kind) {
                return CLASS.equals(kind);
            }

            @Override
            public void transform(final TransformationContext context) {
                String className = context.getTarget().asClass().name().toString();
                if ("org.acme.tck.HelloResourceTest".equals(className)) {
                    context.transform()
                            .add(ApplicationScoped.class)
                            .done();
                }
            }
        }));
    }
}
