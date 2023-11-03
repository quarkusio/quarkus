package io.quarkus.arc.test.event.fire;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class EventFireTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer();

    @Test
    public <T> void testEventFireThrowsExceptionIfEventTypeHasTypeVariable() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> Arc.container().beanManager().getEvent().select(BarInterface.class).fire(new Foo<T>()),
                "Event#fire should throw IllegalArgumentException if the payload contains unresolved type variable");
    }

    public class Foo<T> implements BarInterface {

    }

    public interface BarInterface {

    }
}
