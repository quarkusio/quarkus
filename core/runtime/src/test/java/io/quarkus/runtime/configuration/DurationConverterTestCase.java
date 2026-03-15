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
        assertStandardDurationFormat("PT20M");
        assertStandardDurationFormat("PT20.345S");
        assertStandardDurationFormat("PT15M");
        assertStandardDurationFormat("PT10H");
        assertStandardDurationFormat("P2D");
        assertStandardDurationFormat("P2DT3H4M");
        assertStandardDurationFormat("P2DT3H4M");
        assertStandardDurationFormat("PT-6H3M");
        assertStandardDurationFormat("-PT6H3M");
        assertStandardDurationFormat("-PT-6H+3M");
    }

    private void assertStandardDurationFormat(String durationString) {
        assertEquals(Duration.parse(durationString), durationConverter.convert(durationString));
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

        actualDuration = durationConverter.convert("3D");
        assertEquals(expectedDuration, actualDuration);
    }

    @Test
    public void testValueIsInMillis() {
        Duration expectedDuration = Duration.ofMillis(25);
        Duration actualDuration = durationConverter.convert("25ms");
        assertEquals(expectedDuration, actualDuration);

        actualDuration = durationConverter.convert("25MS");
        assertEquals(expectedDuration, actualDuration);
    }

    @Test
    public void testValueIsInSec() {
        Duration expectedDuration = Duration.ofSeconds(2);
        Duration actualDuration = durationConverter.convert("2s");
        assertEquals(expectedDuration, actualDuration);

        actualDuration = durationConverter.convert("2S");
        assertEquals(expectedDuration, actualDuration);
    }

    @Test
    public void testValueIsInMinutes() {
        Duration expectedDuration = Duration.ofMinutes(2);
        Duration actualDuration = durationConverter.convert("2m");
        assertEquals(expectedDuration, actualDuration);

        actualDuration = durationConverter.convert("2M");
        assertEquals(expectedDuration, actualDuration);
    }

    @Test
    public void testValueIsInHours() {
        Duration expectedDuration = Duration.ofHours(3);
        Duration actualDuration = durationConverter.convert("3h");
        assertEquals(expectedDuration, actualDuration);

        actualDuration = durationConverter.convert("3H");
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
