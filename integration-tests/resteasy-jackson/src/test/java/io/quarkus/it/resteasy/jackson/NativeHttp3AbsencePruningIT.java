package io.quarkus.it.resteasy.jackson;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.nativeimage.ClassInclusionReport;

@QuarkusIntegrationTest
public class NativeHttp3AbsencePruningIT extends GreetingResourceTest {

    @Test
    public void verifyPruning() {
        ClassInclusionReport report = ClassInclusionReport.load();
        report.assertContainsNot("io.netty.handler.codec.quic.Quic");
        report.assertContainsNot("io.netty.handler.codec.quic.Quiche");
    }
}
