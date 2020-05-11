package io.quarkus.arc.test.clientproxy.bridgemethod;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import java.io.IOException;
import java.io.Serializable;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class SameDescriptorDifferentReturnTypeMethodTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(LoopProducer.class, Loop.class, SuperLoop.class);

    @Test
    public void testProxy() throws IOException {
        Serializable ret = Arc.container().instance(SuperLoop.class).get().next();
        assertEquals(9, ret);
    }

    @Dependent
    static class LoopProducer {

        @Produces
        @ApplicationScoped
        Loop produce() {
            return new Loop() {

                @Override
                public Integer next() {
                    return 9;
                }
            };
        }

    }

    interface Loop extends SuperLoop {

        // Since JDK8+ a "Serializable next()" bridge method is also generated 
        Integer next();
    }

    interface SuperLoop {

        Serializable next();

    }

}
