package io.quarkus.vertx.deployment;

import static io.quarkus.arc.processor.Annotations.contains;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.processor.Annotations;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.vertx.runtime.CompletionStageNonBlocking;
import io.quarkus.vertx.runtime.CompletionStageNonBlockingInterceptor;
import io.quarkus.vertx.runtime.UniNonBlocking;
import io.quarkus.vertx.runtime.UniNonBlockingInterceptor;
import io.smallrye.mutiny.Uni;

public class NonBlockingProducerProcessor {

    @BuildStep
    AdditionalBeanBuildItem additionalBeans() {
        return AdditionalBeanBuildItem.builder().addBeanClasses(UniNonBlockingInterceptor.class, UniNonBlocking.class,
                CompletionStageNonBlockingInterceptor.class, CompletionStageNonBlocking.class).build();
    }

    @BuildStep
    AnnotationsTransformerBuildItem annotationTransformer() throws Exception {
        DotName uniName = DotName.createSimple(Uni.class.getName());
        DotName csName = DotName.createSimple(CompletionStage.class.getName());
        DotName cfName = DotName.createSimple(CompletableFuture.class.getName());
        DotName uniNonBlockingName = DotName.createSimple(UniNonBlocking.class.getName());
        DotName csNonBlockingName = DotName.createSimple(CompletionStageNonBlocking.class.getName());
        return new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
            @Override
            public boolean appliesTo(AnnotationTarget.Kind kind) {
                return kind == AnnotationTarget.Kind.METHOD;
            }

            @Override
            public int getPriority() {
                // Make sure this annotation transformer is invoked after the AutoProducerMethodsProcessor.annotationTransformer()
                return DEFAULT_PRIORITY - 2;
            }

            @Override
            public void transform(TransformationContext ctx) {
                Type returnType = ctx.getTarget().asMethod().returnType();
                if (returnType.kind() == org.jboss.jandex.Type.Kind.VOID) {
                    // Skip void methods
                    return;
                }
                Set<AnnotationInstance> methodAnnotations = Annotations.getAnnotations(Kind.METHOD, ctx.getAnnotations());
                if (contains(methodAnnotations, DotNames.PRODUCES)) {
                    if (uniName.equals(returnType.name())) {
                        ctx.transform().add(uniNonBlockingName).done();
                    } else if (csName.equals(returnType.name()) || cfName.equals(returnType.name())) {
                        ctx.transform().add(csNonBlockingName).done();
                    }
                }
            }
        });
    }

}
