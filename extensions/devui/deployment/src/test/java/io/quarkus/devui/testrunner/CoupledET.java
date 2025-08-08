package io.quarkus.devui.testrunner;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CoupledET {

    @Test
    public void unitStyleTest2() {
        Assertions.assertEquals("unit", CoupledService.service());
    }

}
