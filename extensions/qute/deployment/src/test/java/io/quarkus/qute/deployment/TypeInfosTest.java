package io.quarkus.qute.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

import org.junit.jupiter.api.Test;

public class TypeInfosTest {

    @Test
    public void testHintPattern() {
        assertHints("<loop-element>", "<loop-element>");
        assertHints("<set#10><loop-element>", "<set#10>", "<loop-element>");
        assertHints("<set#10><loop#4><any_other>", "<set#10>", "<loop#4>", "<any_other>");
        assertHints("<set#10>loop-element>", "<set#10>");
    }

    private void assertHints(String hintStr, String... expectedHints) {
        Matcher m = TypeInfos.HintInfo.HINT_PATTERN.matcher(hintStr);
        List<String> hints = new ArrayList<>();
        while (m.find()) {
            hints.add(m.group());
        }
        assertEquals(Arrays.asList(expectedHints), hints);
    }

}
