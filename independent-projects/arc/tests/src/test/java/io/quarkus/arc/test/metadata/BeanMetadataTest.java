package io.quarkus.arc.test.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.test.ArcTestContainer;
import javax.enterprise.inject.spi.Bean;
import org.junit.Rule;
import org.junit.Test;

public class BeanMetadataTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(Controller.class);

    @Test
    public void testBeanMetadata() {
        ArcContainer arc = Arc.container();
        Bean<?> bean = arc.instance(Controller.class).get().bean;
        assertNotNull(bean);
        assertEquals(2, bean.getTypes().size());
        assertTrue(bean.getTypes().contains(Controller.class));
        assertTrue(bean.getTypes().contains(Object.class));
    }

}
