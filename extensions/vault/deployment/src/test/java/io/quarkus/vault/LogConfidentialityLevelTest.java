package io.quarkus.vault;

import static io.quarkus.vault.runtime.LogConfidentialityLevel.HIGH;
import static io.quarkus.vault.runtime.LogConfidentialityLevel.LOW;
import static io.quarkus.vault.runtime.LogConfidentialityLevel.MEDIUM;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class LogConfidentialityLevelTest {

    @Test
    public void mask() {
        assertEquals("mypassword", LOW.maskWithTolerance("mypassword", LOW));
        assertEquals("myusername", LOW.maskWithTolerance("myusername", MEDIUM));
        assertEquals("***", MEDIUM.maskWithTolerance("mypassword", LOW));
        assertEquals("myusername", MEDIUM.maskWithTolerance("myusername", MEDIUM));
        assertEquals("***", MEDIUM.maskWithTolerance("mypassword", LOW));
        assertEquals("***", HIGH.maskWithTolerance("mypassword", LOW), "***");
        assertEquals("***", HIGH.maskWithTolerance("myusername", MEDIUM), "***");
    }

}
