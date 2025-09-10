package io.quarkus.it.mockbean;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;

import jakarta.enterprise.event.Event;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.it.mockbean.Foo.Bar;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class MockEventTest {

    @InjectMock
    Event<Foo.Bar> event;

    @Test
    public void testEvent() {
        Foo.Bar bar1 = new Bar(new ArrayList<>());
        event.fire(bar1);
        assertEquals(1, bar1.getNames().size());
        assertEquals("baz", bar1.getNames().get(0));
        
        Mockito.doAnswer(invocation -> {
            Object arg0 = invocation.getArgument(0);
            if (arg0 instanceof Bar bar) {
                bar.getNames().add("bazinga");
            }
            return null;
        }).when(event).fire(Mockito.any(Bar.class));
        Foo.Bar bar2 = new Bar(new ArrayList<>());
        event.fire(bar2);
        // FooBarObserver is not notified this time
        assertEquals(1, bar1.getNames().size());
        assertEquals("bazinga", bar2.getNames().get(0));
    }
}
