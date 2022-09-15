package io.quarkus.arc.test.qualifiers;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests repeating qualifier usage for producers.
 */
public class RepeatingQualifierProducerTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Location.class, Locations.class, NotAQualifier.class,
            SomePlace.class, ProducerBean.class);

    @Test
    public void testRepeatingQualifiers() {
        ArcContainer container = Arc.container();
        container.requestContext().activate();
        // all beans need to be invoked so that they are created, only then can we test disposers work as well
        // simple resolution with just one instance of repeatable qualifier
        InstanceHandle<SomePlace> home = container.instance(SomePlace.class, new Location.Literal("home"));
        Assertions.assertTrue(home.isAvailable());
        home.get().ping();

        // resolution when we select a bean having two repeatable qualifiers but only using one
        InstanceHandle<SomePlace> farAway = container.instance(SomePlace.class, new Location.Literal("farAway"));
        Assertions.assertTrue(farAway.isAvailable());
        farAway.get().ping();

        // resolution where we select a bean having two repeatable qualifiers using both
        InstanceHandle<SomePlace> work = container.instance(SomePlace.class, new Location.Literal("work"),
                new Location.Literal("office"));
        Assertions.assertTrue(work.isAvailable());
        work.get().ping();

        // same as before but backed by field producer
        InstanceHandle<SomePlace> matrix = container.instance(SomePlace.class, new Location.Literal("alternativeReality"),
                new Location.Literal("matrix"));
        Assertions.assertTrue(matrix.isAvailable());
        matrix.get().ping();

        // deactivate req. context which will trigger disposers
        container.requestContext().terminate();

        // assert all disposers were invoked
        ProducerBean producerBean = container.instance(ProducerBean.class).get();
        Assertions.assertTrue(producerBean.isFarAwayDisposerInvoked());
        Assertions.assertTrue(producerBean.isHomeDisposerInvoked());
        Assertions.assertTrue(producerBean.isMatrixDisposerInvoked());
        Assertions.assertTrue(producerBean.isWorkDisposerInvoked());
    }

    @ApplicationScoped
    public static class ProducerBean {

        boolean homeDisposerInvoked = false;
        boolean farAwayDisposerInvoked = false;
        boolean workDisposerInvoked = false;
        boolean matrixDisposerInvoked = false;

        @RequestScoped
        @Produces
        @Location("home")
        @NotAQualifier("ignored")
        public SomePlace produceHome() {
            return new SomePlace() {
                @Override
                public String ping() {
                    return "home";
                }
            };
        }

        @RequestScoped
        @Produces
        @Location("farAway")
        @Location("dreamland")
        @NotAQualifier("ignored")
        public SomePlace producefarAway() {
            return new SomePlace() {
                @Override
                public String ping() {
                    return "work";
                }
            };
        }

        @RequestScoped
        @Produces
        @Location("office")
        @Location("work")
        @NotAQualifier("ignored")
        public SomePlace produceFarWork() {
            return new SomePlace() {
                @Override
                public String ping() {
                    return "farAway";
                }
            };
        }

        // field producer to test that as well
        @RequestScoped
        @Produces
        @Location("matrix")
        @Location("alternativeReality")
        @NotAQualifier("ignored")
        public SomePlace produceMatrix = new SomePlace() {
            @Override
            public String ping() {
                return "matrix";
            }
        };

        public void disposeHome(@Disposes @Location("home") SomePlace s) {
            homeDisposerInvoked = true;
        }

        public void disposeFarAway(@Disposes @Location("farAway") SomePlace s) {
            farAwayDisposerInvoked = true;
        }

        public void disposeWork(@Disposes @Location("work") @Location("office") SomePlace s) {
            workDisposerInvoked = true;
        }

        public void disposeMatrix(@Disposes @Location("matrix") @Location("alternativeReality") SomePlace s) {
            matrixDisposerInvoked = true;
        }

        public boolean isHomeDisposerInvoked() {
            return homeDisposerInvoked;
        }

        public boolean isFarAwayDisposerInvoked() {
            return farAwayDisposerInvoked;
        }

        public boolean isMatrixDisposerInvoked() {
            return matrixDisposerInvoked;
        }

        public boolean isWorkDisposerInvoked() {
            return workDisposerInvoked;
        }
    }
}
