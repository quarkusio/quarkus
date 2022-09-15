package io.quarkus.arc.test.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.inject.spi.Bean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class BeanMetadataTest {

    @RegisterExtension
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
