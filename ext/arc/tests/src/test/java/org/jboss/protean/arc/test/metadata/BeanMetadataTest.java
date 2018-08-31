package org.jboss.protean.arc.test.metadata;

import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;

import org.jboss.protean.arc.Arc;
import org.jboss.protean.arc.ArcContainer;
import org.jboss.protean.arc.test.ArcTestContainer;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class BeanMetadataTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(Controller.class);

    @Test
    public void testBeanMetadata() {
        ArcContainer arc = Arc.container();
        Assert.assertNull(arc.instance(Controller.class).get().bean);
    }

    @SuppressWarnings("serial")
    static class TestLiteral extends AnnotationLiteral<Qualifier> implements Qualifier {

    }

}
