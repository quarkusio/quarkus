package io.quarkus.analytics.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.devtools.messagewriter.MessageWriter;

class GroupIdFilterTest {

    private static final MessageWriter log = MessageWriter.info();

    @Test
    void isAuthorizedGroupId() {
        assertTrue(GroupIdFilter.isAuthorizedGroupId("must.be.authorized", log));
    }

    @Test
    void isDeniedGroupId() {
        assertFalse(GroupIdFilter.isAuthorizedGroupId(null, log));
        assertFalse(GroupIdFilter.isAuthorizedGroupId("", log));
        assertFalse(GroupIdFilter.isAuthorizedGroupId("io.quarkus", log));
        assertFalse(GroupIdFilter.isAuthorizedGroupId("io.quarkus.something", log));
        assertFalse(GroupIdFilter.isAuthorizedGroupId("io.quarkiverse", log));
        assertFalse(GroupIdFilter.isAuthorizedGroupId("io.quarkiverse.something", log));
        assertFalse(GroupIdFilter.isAuthorizedGroupId("org.acme", log));
        assertFalse(GroupIdFilter.isAuthorizedGroupId("org.acme.something", log));
    }
}
