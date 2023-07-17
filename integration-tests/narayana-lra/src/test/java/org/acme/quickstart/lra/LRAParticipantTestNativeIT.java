package org.acme.quickstart.lra;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusIntegrationTest
@TestProfile(AppTestProfile.class)
public class LRAParticipantTestNativeIT extends LRAParticipantTest {
}
