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
        Assertions.assertThrows(IllegalArgumentException.class,
                this::dotestEventSelectThrowsExceptionIfEventTypeHasTypeVariable,
                "Event#select should throw IllegalArgumentException if the event uses type variable");
    }

    private <T> void dotestEventSelectThrowsExceptionIfEventTypeHasTypeVariable() {
        SecuritySensor sensor = Arc.container().select(SecuritySensor.class).get();
        sensor.securityEvent.select(new TypeLiteral<SecurityEvent_Illegal<T>>() {
        });
    }

    @Test
    public void testEventSelectThrowsExceptionForDuplicateBindingType() {
        Assertions.assertThrows(IllegalArgumentException.class,
                this::dotestEventSelectThrowsExceptionForDuplicateBindingType,
                "Event#select should throw IllegalArgumentException when there are duplicate bindings specified.");
    }

    private void dotestEventSelectThrowsExceptionForDuplicateBindingType() {
        SecuritySensor sensor = Arc.container().select(SecuritySensor.class).get();
        sensor.securityEvent.select(new SystemTest.SystemTestLiteral("a") {
        }, new SystemTest.SystemTestLiteral("b") {
        });
    }

    @Test
    public void testEventSelectWithSubtypeThrowsExceptionForDuplicateBindingType() {
        Assertions.assertThrows(IllegalArgumentException.class,
                this::dotestEventSelectWithSubtypeThrowsExceptionForDuplicateBindingType,
                "Event#select should throw IllegalArgumentException when selecting a subtype with duplicate bindings.");
    }

    private void dotestEventSelectWithSubtypeThrowsExceptionForDuplicateBindingType() {
        SecuritySensor sensor = Arc.container().select(SecuritySensor.class).get();
        sensor.securityEvent.select(BreakInEvent.class, new SystemTest.SystemTestLiteral("a") {
        }, new SystemTest.SystemTestLiteral("b") {
        });
    }

    @Test
    public void testEventSelectThrowsExceptionIfAnnotationIsNotBindingType() {
        Assertions.assertThrows(IllegalArgumentException.class,
                this::dotestEventSelectThrowsExceptionIfAnnotationIsNotBindingType,
                "Event#select should throw IllegalArgumentException if the annotation is not a binding type.");
    }

    private void dotestEventSelectThrowsExceptionIfAnnotationIsNotBindingType() {
        SecuritySensor sensor = Arc.container().select(SecuritySensor.class).get();
        sensor.securityEvent.select(new AnnotationLiteral<NotABindingType>() {
        });
    }

    @Test
    public void testEventSelectWithSubtypeThrowsExceptionIfAnnotationIsNotBindingType() {
        Assertions.assertThrows(IllegalArgumentException.class,
                this::dotestEventSelectWithSubtypeThrowsExceptionIfAnnotationIsNotBindingType,
                "Event#select should throw IllegalArgumentException when selecting a subtype and using annotation that is not a binding type.");
    }

    private void dotestEventSelectWithSubtypeThrowsExceptionIfAnnotationIsNotBindingType() {
        SecuritySensor sensor = Arc.container().select(SecuritySensor.class).get();
        sensor.securityEvent.select(BreakInEvent.class, new AnnotationLiteral<NotABindingType>() {
        });
    }
}
