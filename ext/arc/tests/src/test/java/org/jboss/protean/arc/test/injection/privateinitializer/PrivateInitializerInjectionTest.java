package org.jboss.protean.arc.test.injection.privateinitializer;

import static org.junit.Assert.assertNotNull;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.jboss.protean.arc.Arc;
import org.jboss.protean.arc.test.ArcTestContainer;
import org.junit.Rule;
import org.junit.Test;

public class PrivateInitializerInjectionTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(Head.class, CombineHarvester.class);

    @Test
    public void testInjection() {
        assertNotNull(Arc.container().instance(CombineHarvester.class).get().getHead());
    }

    @Dependent
    static class Head {

    }

    @ApplicationScoped
    static class CombineHarvester {

        private Head head;

        @Inject
        private void setHead(Head head) {
            this.head = head;
        }

        public Head getHead() {
            return head;
        }

    }
}
