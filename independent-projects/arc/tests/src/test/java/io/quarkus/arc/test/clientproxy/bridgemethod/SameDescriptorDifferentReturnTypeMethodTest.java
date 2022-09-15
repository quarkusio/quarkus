package io.quarkus.arc.test.clientproxy.bridgemethod;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import java.io.IOException;
import java.io.Serializable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class SameDescriptorDifferentReturnTypeMethodTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(LoopProducer.class, Loop.class, SuperLoop.class,
            EvenMoreSuperLoop.class, ComplexLoop.class, Complex.class);

    @Test
    public void testProxy() throws IOException {
        Serializable ret = Arc.container().instance(SuperLoop.class).get().next();
        assertEquals(9, ret);
        Object complexRet = Arc.container().instance(EvenMoreSuperLoop.class, new Complex.Literal()).get().next();
        assertEquals(11, complexRet);
    }

    @Dependent
    static class LoopProducer {

        @Produces
        @ApplicationScoped
        Loop produceLoop() {
            return new Loop() {

                @Override
                public Integer next() {
                    return 9;
                }
            };
        }

        @Complex
        @Produces
        @ApplicationScoped
        ComplexLoop produceComplexLoop() {
            return new ComplexLoop() {

                @Override
                public Integer next() {
                    return 11;
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

    interface EvenMoreSuperLoop {

        Object next();

    }

    interface ComplexLoop extends SuperLoop, EvenMoreSuperLoop {

        // intentionally don't implement next()

    }

}
