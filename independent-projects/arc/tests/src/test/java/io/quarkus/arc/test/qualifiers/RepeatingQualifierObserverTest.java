package io.quarkus.arc.test.qualifiers;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class RepeatingQualifierObserverTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Location.class, Locations.class, NotAQualifier.class,
            SomePlace.class, ObservingBean.class);

    @Test
    public void testRepeatingQualifiers() {
        ArcContainer container = Arc.container();
        Event<String> event = container.beanManager().getEvent().select(String.class);
        ObservingBean bean = container.instance(ObservingBean.class).get();

        List<String> expectedList = new ArrayList<>();
        assertListsAreEqual(expectedList, bean.getEvents());
        event.select(new Location.Literal("home")).fire("home");
        expectedList.add("home");
        assertListsAreEqual(expectedList, bean.getEvents());
        event.select(new Location.Literal("farAway"), new Location.Literal("dreamland")).fire("farAway");
        expectedList.add("farAway");
        assertListsAreEqual(expectedList, bean.getEvents());
        event.select(new Location.Literal("work"), new Location.Literal("office")).fire("work");
        expectedList.add("work");
        assertListsAreEqual(expectedList, bean.getEvents());
    }

    private void assertListsAreEqual(List<String> expected, List<String> actual) {
        Assertions.assertTrue(expected.size() == actual.size());
        for (int i = 0; i < expected.size(); i++) {
            Assertions.assertEquals(expected.get(i), actual.get(i));
        }
    }

    @ApplicationScoped
    public static class ObservingBean {

        List<String> events = new ArrayList<>();

        public void observeHome(@Observes @Location("home") String s) {
            events.add(s);
        }

        public void observeFarAway(@Observes @Location("farAway") String s) {
            events.add(s);
        }

        public void observeWork(@Observes @Location("work") @Location("office") String s) {
            events.add(s);
        }

        public List<String> getEvents() {
            return events;
        }
    }

}
