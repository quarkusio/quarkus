package io.quarkus.arc.test.event.select;

import jakarta.enterprise.util.TypeLiteral;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

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
        SecuritySensor sensor = Arc.container().select(SecuritySensor.class).get();
        TypeLiteral<SecurityEvent_Illegal<T>> typeLiteral = new TypeLiteral<SecurityEvent_Illegal<T>>() {
        };
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> {
                    sensor.securityEvent.select(typeLiteral);
                }, "Event#select should throw IllegalArgumentException if the event uses type variable");

        // do the same but with raw type passed into select(Class<U> subtype, Annotation ... qualifiers)
        Assertions.assertThrows(IllegalArgumentException.class, () -> sensor.securityEvent.select(SecurityEvent_Illegal.class),
                "Event#select should throw IllegalArgumentException if the event uses type variable");
    }

    @Test
    public void testEventSelectThrowsExceptionForDuplicateBindingType() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            SecuritySensor sensor = Arc.container().select(SecuritySensor.class).get();
            sensor.securityEvent.select(new SystemTest.SystemTestLiteral("a") {
            },
                    new SystemTest.SystemTestLiteral("b") {
                    });
        }, "Event#select should throw IllegalArgumentException when there are duplicate bindings specified.");
    }

    @Test
    public void testEventSelectWithSubtypeThrowsExceptionForDuplicateBindingType() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            SecuritySensor sensor = Arc.container().select(SecuritySensor.class).get();
            sensor.securityEvent.select(BreakInEvent.class, new SystemTest.SystemTestLiteral("a") {
            },
                    new SystemTest.SystemTestLiteral("b") {
                    });
        }, "Event#select should throw IllegalArgumentException when selecting a subtype with duplicate bindings.");
    }

    @Test
    public void testEventSelectThrowsExceptionIfAnnotationIsNotBindingType() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            SecuritySensor sensor = Arc.container().select(SecuritySensor.class).get();
            sensor.securityEvent.select(new NotABindingType.Literal());
        }, "Event#select should throw IllegalArgumentException if the annotation is not a binding type.");
    }

    @Test
    public void testEventSelectWithSubtypeThrowsExceptionIfAnnotationIsNotBindingType() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            SecuritySensor sensor = Arc.container().select(SecuritySensor.class).get();
            sensor.securityEvent.select(BreakInEvent.class, new NotABindingType.Literal());
        }, "Event#select should throw IllegalArgumentException when selecting a subtype and using annotation that is not a binding type.");
    }
}
