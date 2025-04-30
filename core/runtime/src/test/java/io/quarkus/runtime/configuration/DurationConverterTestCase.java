package io.quarkus.runtime.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DurationConverterTestCase {

    private DurationConverter durationConverter;

    @BeforeEach
    public void setup() {
        durationConverter = new DurationConverter();
    }

    @Test
    public void testOnlyNumberValueProvided() {
        Duration expectedDuration = Duration.ofSeconds(3);
        Duration actualDuration = durationConverter.convert("3");
        assertEquals(expectedDuration, actualDuration);
    }

    @Test
    public void testValueStartingWithNumberAndInCorrectFormatProvided() {
        Duration expectedDuration = Duration.parse("PT21.345S");
        Duration actualDuration = durationConverter.convert("21.345S");
        assertEquals(expectedDuration, actualDuration);
    }

    @Test
    public void testValueStartingWithNumberAndNotInCorrectFormatProvided() {
        assertThrows(IllegalArgumentException.class, () -> durationConverter.convert("21.X345S"));
    }

    @Test
    public void testValueInCorrectFormatProvided() {
        Duration expectedDuration = Duration.parse("PT20M");
        Duration actualDuration = durationConverter.convert("PT20M");
        assertEquals(expectedDuration, actualDuration);
    }

    @Test
    public void testValueNotInCorrectFormatProvided() {
        assertThrows(IllegalArgumentException.class, () -> durationConverter.convert("PT"));
    }

    @Test
    public void testValueIsInDays() {
        Duration expectedDuration = Duration.ofDays(3);
        Duration actualDuration = durationConverter.convert("3d");
        assertEquals(expectedDuration, actualDuration);
    }

    @Test
    public void testValueIsInMillis() {
        Duration expectedDuration = Duration.ofMillis(25);
        Duration actualDuration = durationConverter.convert("25ms");
        assertEquals(expectedDuration, actualDuration);
    }

    @Test
    public void testValueIsInSec() {
        Duration expectedDuration = Duration.ofSeconds(2);
        Duration actualDuration = durationConverter.convert("2s");
        assertEquals(expectedDuration, actualDuration);
    }

    @Test
    public void testValuesWithMultipleUnits() {
        Duration expectedDuration = Duration.ofSeconds(150);
        Duration actualDuration = durationConverter.convert("2m30s");
        assertEquals(expectedDuration, actualDuration);
    }

    @Test
    public void testValuesWithMultipleUnitsSigned() {
        Duration expectedDuration = Duration.ofSeconds(90);
        Duration actualDuration = durationConverter.convert("+2m-30s");
        assertEquals(expectedDuration, actualDuration);
    }
}
