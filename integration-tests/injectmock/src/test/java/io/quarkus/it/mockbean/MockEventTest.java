package io.quarkus.it.mockbean;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.it.mockbean.Foo.Bar;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class MockEventTest {

    @Inject
    Event<Foo.Bar> event1;

    @InjectMock
    Event<Foo.Bar> event2;

    @Inject
    FooBarObserver fooBarObserver;

    @Test
    public void testEvent() {
        Mockito.doAnswer(invocation -> {
            Object arg0 = invocation.getArgument(0);
            if (arg0 instanceof Bar bar) {
                bar.getNames().add("bazinga");
            }
            return null;
        }).when(event2).fire(Mockito.any(Bar.class));

        // FooBarObserver is not notified because @Inject Event<Foo.Bar> also delegates to the mock
        Foo.Bar bar1 = new Bar(new ArrayList<>());
        event1.fire(bar1);
        assertEquals(1, bar1.getNames().size());
        assertEquals("bazinga", bar1.getNames().get(0));

        Foo.Bar bar2 = new Bar(new ArrayList<>());
        event2.fire(bar2);
        assertEquals(1, bar1.getNames().size());
        assertEquals("bazinga", bar2.getNames().get(0));

        // Event injected in FooBarObserver also delegates to the mock
        Foo.Bar bar3 = fooBarObserver.fireBar();
        assertEquals(1, bar3.getNames().size());
        assertEquals("bazinga", bar3.getNames().get(0));
    }
}
