package io.quarkus.arc.test.event.select;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import javax.enterprise.util.AnnotationLiteral;
import javax.enterprise.util.TypeLiteral;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests that event selection throws exceptions under certain circumstances.
 */
public class EventSelectTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(BreakInEvent.class, NotABindingType.class,
            SecurityEvent.class, SecurityEvent_Illegal.class,
            SecuritySensor.class, SystemTest.class);

    @Test
    public <T> void testEventSelectThrowsExceptionIfEventTypeHasTypeVariable() {
        try {
            SecuritySensor sensor = Arc.container().select(SecuritySensor.class).get();
            sensor.securityEvent.select(new TypeLiteral<SecurityEvent_Illegal<T>>() {
            });
            Assertions.fail("Event#select should throw IllegalArgumentException if the event uses type variable");
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    @Test
    public void testEventSelectThrowsExceptionForDuplicateBindingType() {
        try {
            SecuritySensor sensor = Arc.container().select(SecuritySensor.class).get();
            sensor.securityEvent.select(new SystemTest.SystemTestLiteral("a") {
            },
                    new SystemTest.SystemTestLiteral("b") {
                    });
            Assertions.fail("Event#select should throw IllegalArgumentException when there are duplicate bindings specified.");
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    @Test
    public void testEventSelectWithSubtypeThrowsExceptionForDuplicateBindingType() {
        try {
            SecuritySensor sensor = Arc.container().select(SecuritySensor.class).get();
            sensor.securityEvent.select(BreakInEvent.class, new SystemTest.SystemTestLiteral("a") {
            },
                    new SystemTest.SystemTestLiteral("b") {
                    });
            Assertions.fail(
                    "Event#select should throw IllegalArgumentException when selecting a subtype with duplicate bindings.");
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    @Test
    public void testEventSelectThrowsExceptionIfAnnotationIsNotBindingType() {
        try {
            SecuritySensor sensor = Arc.container().select(SecuritySensor.class).get();
            sensor.securityEvent.select(new AnnotationLiteral<NotABindingType>() {
            });
            Assertions.fail("Event#select should throw IllegalArgumentException if the annotation is not a binding type.");
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    @Test
    public void testEventSelectWithSubtypeThrowsExceptionIfAnnotationIsNotBindingType() {
        try {
            SecuritySensor sensor = Arc.container().select(SecuritySensor.class).get();
            sensor.securityEvent.select(BreakInEvent.class, new AnnotationLiteral<NotABindingType>() {
            });
            Assertions.fail(
                    "Event#select should throw IllegalArgumentException when selecting a subtype and using annotation that is not a binding type.");
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }
}
