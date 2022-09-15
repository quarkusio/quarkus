package io.quarkus.arc.test.qualifiers;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests usage of repeated qualifiers for class based beans.
 */
public class RepeatingQualifierClassTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Location.class, Locations.class, SomePlace.class, Home.class,
            FarAway.class, Work.class, NotAQualifier.class, InjectingBean.class);

    @Test
    public void testRepeatingQualifiers() {
        ArcContainer container = Arc.container();
        // simple resolution with just one instance of repeatable qualifier
        InstanceHandle<SomePlace> home = container.instance(SomePlace.class, new Location.Literal("home"));
        Assertions.assertTrue(home.isAvailable());

        // resolution when we select a bean having two repeatable qualifiers but only using one
        InstanceHandle<SomePlace> farAway = container.instance(SomePlace.class, new Location.Literal("farAway"));
        Assertions.assertTrue(farAway.isAvailable());

        // resolution where we select a bean having two repeatable qualifiers using both
        InstanceHandle<SomePlace> work = container.instance(SomePlace.class, new Location.Literal("work"),
                new Location.Literal("office"));
        Assertions.assertTrue(work.isAvailable());

        InjectingBean injectingBean = container.instance(InjectingBean.class).get();
        Assertions.assertNotNull(injectingBean.getFarAway());
        Assertions.assertNotNull(injectingBean.getHome());
        Assertions.assertNotNull(injectingBean.getWork());
        Assertions.assertNotNull(injectingBean.getLocationFromInitializer());
    }

    @Singleton
    @Location("home")
    @NotAQualifier("ignored")
    public static class Home implements SomePlace {

        public String ping() {
            return "home";
        }
    }

    @Singleton
    @Location("farAway")
    @Location("dreamland")
    @NotAQualifier("ignored")
    public static class FarAway implements SomePlace {

        public String ping() {
            return "farAway";
        }
    }

    @Singleton
    @Location("work")
    @Location("office")
    @NotAQualifier("ignored")
    public static class Work implements SomePlace {

        public String ping() {
            return "work";
        }
    }

    @ApplicationScoped
    public static class InjectingBean {

        private SomePlace methodInjection;

        // test method injection
        @Inject
        public void init(@Location("work") @Location("office") SomePlace place) {
            this.methodInjection = place;
        }

        //test field injection
        @Inject
        @Location("home")
        SomePlace home;

        @Inject
        @Location("farAway")
        @Location("dreamland")
        SomePlace farAway;

        @Inject
        @Location("work")
        @Location("office")
        SomePlace work;

        public SomePlace getHome() {
            return home;
        }

        public SomePlace getFarAway() {
            return farAway;
        }

        public SomePlace getWork() {
            return work;
        }

        public SomePlace getLocationFromInitializer() {
            return methodInjection;
        }
    }
}
