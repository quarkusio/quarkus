package io.quarkus.observability.test;

import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.observability.devresource.lgtm.LgtmResource;
import io.quarkus.observability.test.support.QuarkusTestResourceTestProfile;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@WithTestResource(LgtmResource.class)
@TestProfile(QuarkusTestResourceTestProfile.class)
@DisabledOnOs(OS.WINDOWS)
public class LgtmLifecycleTest extends LgtmTestBase {
}
