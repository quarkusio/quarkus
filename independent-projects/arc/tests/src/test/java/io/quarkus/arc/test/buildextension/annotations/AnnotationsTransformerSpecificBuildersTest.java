package io.quarkus.arc.test.buildextension.annotations;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Vetoed;
import jakarta.inject.Inject;

import org.jboss.jandex.PrimitiveType;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.test.ArcTestContainer;

public class AnnotationsTransformerSpecificBuildersTest extends AbstractTransformerBuilderTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(Seven.class, One.class, IWantToBeABean.class)
            .annotationTransformations(
                    AnnotationsTransformer.appliedToClass()
                            .whenContainsAny(Dependent.class)
                            .whenClass(c -> c.name().toString().equals(One.class.getName()))
                            .thenTransform(t ->
                            // Veto bean class One
                            t.add(Vetoed.class)),

                    AnnotationsTransformer.appliedToClass()
                            .whenClass(c -> c.name().local()
                                    .equals(IWantToBeABean.class.getSimpleName()))
                            .transform(context -> context.transform().add(Dependent.class).done()),
                    AnnotationsTransformer.appliedToField()
                            .whenField(f -> f.name().equals("seven"))
                            .thenTransform(t -> t.add(Inject.class)),

                    // Add @Produces to a method that returns int and is not annoated with @Simple
                    AnnotationsTransformer.appliedToMethod()
                            .whenContainsNone(Simple.class)
                            .whenMethod(m -> m.returnType().name().equals(PrimitiveType.INT.name()))
                            .thenTransform(t -> t.add(Produces.class)))
            .build();

}
