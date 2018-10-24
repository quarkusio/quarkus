package org.jboss.protean.arc.test.build.processor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.AbstractList;
import java.util.Collection;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Vetoed;
import javax.inject.Inject;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.protean.arc.Arc;
import org.jboss.protean.arc.ArcContainer;
import org.jboss.protean.arc.InstanceHandle;
import org.jboss.protean.arc.processor.AnnotationsTransformer;
import org.jboss.protean.arc.processor.Transformation;
import org.jboss.protean.arc.test.ArcTestContainer;
import org.junit.Rule;
import org.junit.Test;

public class AnnotationsTransformerTest {

    @Rule
    public ArcTestContainer container = ArcTestContainer.builder().beanClasses(Seven.class, One.class, IWantToBeABean.class)
            .annotationsTransformers(new MyTransformer()).build();

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
        public boolean appliesTo(Kind kind) {
            return kind == Kind.CLASS || kind == Kind.FIELD;
        }

        @Override
        public Collection<AnnotationInstance> transform(AnnotationTarget target, Collection<AnnotationInstance> annotations) {
            if (target.kind() == Kind.CLASS) {
                if (target.asClass().name().toString().equals(One.class.getName())) {
                    // Veto bean class One
                    return Transformation.with(target, annotations).add(Vetoed.class).done();
                }
                if (target.asClass().name().local().equals(IWantToBeABean.class.getSimpleName())) {
                    return Transformation.with(target, annotations).add(Dependent.class).done();
                }
            } else if (target.kind() == Kind.FIELD && target.asField().name().equals("seven")) {
                return Transformation.with(target, annotations).add(Inject.class).done();
            }
            return annotations;
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
