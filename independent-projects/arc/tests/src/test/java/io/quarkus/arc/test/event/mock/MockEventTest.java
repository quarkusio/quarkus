package io.quarkus.arc.test.event.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

import io.quarkus.arc.Arc;
import io.quarkus.arc.impl.Mockable;
import io.quarkus.arc.test.ArcTestContainer;

public class MockEventTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(EventConsumer.class)
            .additionalClasses(Drink.class)
            .testMode(true)
            .build();

    @Test
    public void testEvent() {
        Arc.container().select(EventConsumer.class).get().installMockAndFire();
        assertEquals("foo", EventConsumer.drinkName);
    }

    public record Drink(String name) {
    }

    @Singleton
    public static class EventConsumer {

        static String drinkName;

        @Inject
        Event<Drink> event;

        @SuppressWarnings("unchecked")
        public void installMockAndFire() {
            assertInstanceOf(Mockable.class, event);
            if (event instanceof Mockable mockable) {
                Event<Drink> mock = Mockito.mock(Event.class);
                Mockito.doAnswer(invocation -> {
                    Object arg0 = invocation.getArgument(0);
                    if (arg0 instanceof Drink drink) {
                        drinkName = drink.name();
                    }
                    return null;
                }).when(mock).fire(new Drink("foo"));
                mockable.arc$setMock(mock);
            }
            event.fire(new Drink("foo"));
        }

    }
}
