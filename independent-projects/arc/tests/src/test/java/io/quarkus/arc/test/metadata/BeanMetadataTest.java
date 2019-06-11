package io.quarkus.arc.test.metadata;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.test.ArcTestContainer;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;
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
