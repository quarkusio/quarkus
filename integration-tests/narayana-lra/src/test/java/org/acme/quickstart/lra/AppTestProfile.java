package org.acme.quickstart.lra;

import java.util.List;

import io.quarkus.test.junit.QuarkusTestProfile;

// we use a QuarkusTestProfile to ensure that QuarkusIntegrationTest works properly with them
public class AppTestProfile implements QuarkusTestProfile {

    @Override
    public List<TestResourceEntry> testResources() {
        return List.of(new TestResourceEntry(LRAParticipantTestResourceLifecycle.class));
    }
}
