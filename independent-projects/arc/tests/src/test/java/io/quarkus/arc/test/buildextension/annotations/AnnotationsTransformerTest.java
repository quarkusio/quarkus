package io.quarkus.arc.test.buildextension.annotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Vetoed;
import jakarta.inject.Inject;
import java.util.AbstractList;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.DotName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class AnnotationsTransformerTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(Seven.class, One.class, IWantToBeABean.class)
            .annotationsTransformers(new MyTransformer(), new DisabledTransformer()).build();

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
    }

    static class MyTransformer implements AnnotationsTransformer {

        @Override
        public boolean initialize(BuildContext buildContext) {
            assertNotNull(buildContext.get(Key.INDEX).getClassByName(DotName.createSimple(IWantToBeABean.class.getName())));
            return true;
        }

        @Override
        public boolean appliesTo(Kind kind) {
            return kind == Kind.CLASS || kind == Kind.FIELD;
        }

        @Override
        public void transform(TransformationContext context) {
            if (context.isClass()) {
                if (context.getTarget().asClass().name().toString().equals(One.class.getName())) {
                    // Veto bean class One
                    context.transform().add(Vetoed.class).done();
                }
                if (context.getTarget().asClass().name().local().equals(IWantToBeABean.class.getSimpleName())) {
                    context.transform().add(Dependent.class).done();
                }
            } else if (context.isField() && context.getTarget().asField().name().equals("seven")) {
                context.transform().add(Inject.class).done();
            }
        }

    }

    static class DisabledTransformer implements AnnotationsTransformer {

        @Override
        public boolean initialize(BuildContext buildContext) {
            return false;
        }

        @Override
        public void transform(TransformationContext transformationContext) {
        }

    }

    // => add @Dependent here
    static class IWantToBeABean {

        // => add @Inject here
        Seven seven;

        public int size() {
            return seven.size();
        }

    }

    @Dependent
    static class Seven extends AbstractList<Integer> {

        @Override
        public Integer get(int index) {
            return Integer.valueOf(7);
        }

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
