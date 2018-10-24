package org.jboss.protean.arc.test.build.processor;

import static org.junit.Assert.assertTrue;

import javax.enterprise.context.Dependent;

import org.jboss.protean.arc.Arc;
import org.jboss.protean.arc.test.ArcTestContainer;
import org.junit.Rule;
import org.junit.Test;

public class DeploymentEnhancerTest {

    @Rule
    public ArcTestContainer container = ArcTestContainer.builder().deploymentEnhancers(dc -> dc.addClass(Fool.class)).build();

    @Test
    public void testEnhancer() {
        assertTrue(Arc.container().instance(Fool.class).isAvailable());
    }

    // => this class is not part of the original deployment
    @Dependent
    static class Fool {

    }

}
