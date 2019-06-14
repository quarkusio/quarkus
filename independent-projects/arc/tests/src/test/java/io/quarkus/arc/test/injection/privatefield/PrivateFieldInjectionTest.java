package io.quarkus.arc.test.injection.privatefield;

import static org.junit.Assert.assertNotNull;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import org.junit.Rule;
import org.junit.Test;

public class PrivateFieldInjectionTest {

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

        @Inject
        private Head head;

        public Head getHead() {
            return head;
        }

    }
}
