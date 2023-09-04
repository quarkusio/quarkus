package org.acme;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class HelloPlainTest {

    @Test
    public void testHolder() {
        System.out.println("HOLLY plain test re-running! " + new Holder().getThing());
        assertEquals("thing", new Holder().getThing());
    }

}
