package io.quarkus.arc.test.buildextension.annotations;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Vetoed;
import jakarta.inject.Inject;

import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.PrimitiveType;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.test.ArcTestContainer;

public class AnnotationsTransformerBuilderTest extends AbstractTransformerBuilderTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(Seven.class, One.class, IWantToBeABean.class)
            .annotationTransformations(
                    AnnotationsTransformer.builder()
                            .appliesTo(Kind.CLASS)
                            .whenContainsAny(Dependent.class)
                            .transform(context -> {
                                if (context.getTarget().asClass().name().toString().equals(One.class.getName())) {
                                    // Veto bean class One
                                    context.transform().add(Vetoed.class).done();
                                }
                            }),
                    AnnotationsTransformer.builder()
                            .appliesTo(Kind.CLASS)
                            .when(context -> context.getTarget().asClass().name().local()
                                    .equals(IWantToBeABean.class.getSimpleName()))
                            .transform(context -> context.transform().add(Dependent.class).done()),
                    AnnotationsTransformer.builder()
                            .appliesTo(Kind.FIELD)
                            .transform(context -> {
                                if (context.getTarget().asField().name().equals("seven")) {
                                    context.transform().add(Inject.class).done();
                                }
                            }),
                    // Add @Produces to a method that returns int and is not annoated with @Simple
                    AnnotationsTransformer.builder()
                            .appliesTo(Kind.METHOD)
                            .whenContainsNone(Simple.class)
                            .when(context -> context.getTarget().asMethod().returnType().name()
                                    .equals(PrimitiveType.INT.name()))
                            .transform(context -> {
                                context.transform().add(Produces.class).done();
                            }),

                    AnnotationsTransformer.appliedToMethod()
                            .whenContainsNone(Simple.class)
                            .whenMethod(m -> m.returnType().name().equals(PrimitiveType.INT.name()))
                            .transform(context -> context.transform().add(Produces.class).done())

            )

            .build();

}
