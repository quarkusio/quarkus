package io.quarkus.arc.test.buildextension.annotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Vetoed;
import jakarta.inject.Inject;
import java.util.AbstractList;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.PrimitiveType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class AnnotationsTransformerBuilderTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(Seven.class, One.class, IWantToBeABean.class)
            .annotationsTransformers(
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
                            }))
            .build();

    @Test
    public void testVetoed() {
        ArcContainer arc = Arc.container();
        assertTrue(arc.instance(Seven.class).isAvailable());
        // One is vetoed
        assertFalse(arc.instance(One.class).isAvailable());
        assertEquals(Integer.valueOf(7), Integer.valueOf(arc.instance(Seven.class).get().size()));

        // Scope annotation and @Inject are added by transformer
        InstanceHandle<IWantToBeABean> iwant = arc.instance(IWantToBeABean.class);
        assertTrue(iwant.isAvailable());
        assertEquals(Integer.valueOf(7), Integer.valueOf(iwant.get().size()));

        assertEquals(Integer.valueOf(7), arc.select(int.class).get());
    }

    // => add @Dependent here
    static class IWantToBeABean {

        // => add @Inject here
        Seven seven;

        // => add @Produces here
        public int size() {
            return seven.size();
        }

        // => do not add @Produces here
        @Simple
        public int anotherSize() {
            return seven.size();
        }

    }

    @Dependent
    static class Seven extends AbstractList<Integer> {

        @Override
        public Integer get(int index) {
            return Integer.valueOf(7);
        }

        @Simple
        @Override
        public int size() {
            return 7;
        }

    }

    // => add @Vetoed here
    @Dependent
    static class One extends AbstractList<Integer> {

        @Override
        public Integer get(int index) {
            return Integer.valueOf(1);
        }

        @Override
        public int size() {
            return 1;
        }

    }

}
