package org.jboss.protean.arc.test.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.protean.arc.Arc;
import org.jboss.protean.arc.ArcContainer;
import org.jboss.protean.arc.test.ArcTestContainer;
import org.junit.Rule;
import org.junit.Test;

public class InjectionPointMetadataTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(Controller.class, Controlled.class);

    @Test
    public void testInjectionPointMetadata() {
        ArcContainer arc = Arc.container();
        Controller controller = arc.instance(Controller.class).get();
        InjectionPoint injectionPoint = controller.controlled.injectionPoint;
        assertNotNull(injectionPoint);
        assertEquals(Controlled.class, injectionPoint.getType());
    }

    @Singleton
    static class Controller {

        @Inject
        Controlled controlled;

    }

    @Dependent
    static class Controlled {

        @Inject
        InjectionPoint injectionPoint;

    }

}
