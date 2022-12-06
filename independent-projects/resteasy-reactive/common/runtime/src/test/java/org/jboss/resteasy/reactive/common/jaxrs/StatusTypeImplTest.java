package org.jboss.resteasy.reactive.common.jaxrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

public class StatusTypeImplTest {
    @Test
    public void testEquals() {
        Response.StatusType statusType = new StatusTypeImpl(200, null);
        assertEquals(statusType, Response.Status.OK);
    }

    @Test
    public void testNotEquals() {
        Response.StatusType statusType = new StatusTypeImpl(200, "All works OK");
        assertNotEquals(statusType, Response.Status.OK);
    }
}
