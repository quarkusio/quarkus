package io.quarkus.arc.test.injection.constructornoinject;

import static org.junit.Assert.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import javax.inject.Singleton;
import org.junit.Rule;
import org.junit.Test;

public class NoArgConstructorTakesPrecedenceTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(CombineHarvester.class);

    @Test
    public void testInjection() {
        assertEquals("OK", Arc.container().instance(CombineHarvester.class).get().getHead());
    }

    @Singleton
    static class CombineHarvester {

        private String head;

        public CombineHarvester() {
            this.head = "OK";
        }

        public CombineHarvester(String head) {
            this.head = head;
        }

        public String getHead() {
            return head;
        }

    }
}
