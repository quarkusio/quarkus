package io.quarkus.qute.deployment;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

public class QuteProcessorTest {

    @Test
    public void testTemplateDataIgnorePattern() {
        List<String> names = List.of("foo", "bar");
        Pattern p = Pattern.compile(QuteProcessor.buildIgnorePattern(names));
        // Ignore "baz" and "getFoo"
        assertTrue(p.matcher("baz").matches());
        assertTrue(p.matcher("getFoo").matches());
        // Do not ignore "foo" and "bar"
        for (String name : names) {
            assertFalse(p.matcher(name).matches());
        }
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> QuteProcessor.buildIgnorePattern(List.of()));
    }

}
