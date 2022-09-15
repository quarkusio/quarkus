package io.quarkus.arc.test.injection.constructornoinject;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class NoArgConstructorTakesPrecedenceTest {

    @RegisterExtension
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
