package io.quarkus.observability.test;

import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.observability.devresource.victoriametrics.VictoriaMetricsResource;
import io.quarkus.observability.devresource.vmagent.VMAgentResource;
import io.quarkus.observability.test.support.QuarkusTestResourceTestProfile;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@QuarkusTestResource.List({
        @QuarkusTestResource(value = VictoriaMetricsResource.class, restrictToAnnotatedClass = true),
        @QuarkusTestResource(value = VMAgentResource.class, restrictToAnnotatedClass = true) })
@TestProfile(QuarkusTestResourceTestProfile.class)
@DisabledOnOs(OS.WINDOWS)
public class QuarkusTestResourceMetricsTest extends MetricsTestBase {
}
