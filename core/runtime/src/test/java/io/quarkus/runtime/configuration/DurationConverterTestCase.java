package io.quarkus.runtime.configuration;

import static org.junit.Assert.assertEquals;

import java.time.Duration;

import org.junit.Before;
import org.junit.Test;

public class DurationConverterTestCase {

    private DurationConverter durationConverter;

    @Before
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

    @Test(expected = IllegalArgumentException.class)
    public void testValueStartingWithNumberAndNotInCorrectFormatProvided() {
        durationConverter.convert("21.X345S");
    }

    @Test
    public void testValueInCorrectFormatProvided() {
        Duration expectedDuration = Duration.parse("PT20M");
        Duration actualDuration = durationConverter.convert("PT20M");
        assertEquals(expectedDuration, actualDuration);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValueNotInCorrectFormatProvided() {
        durationConverter.convert("PT");
    }
}
