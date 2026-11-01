package io.quarkus.grpc.examples.stress;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

/**
 * Large-payload stress with an enlarged HTTP/2 connection window (unified Vert.x server).
 */
@QuarkusTest
@TestProfile(LargeHttp2WindowStressTest.Profile.class)
class LargeHttp2WindowStressTest {

    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.http.http2-connection-window-size", "104857600");
        }
    }

    @GrpcClient("stress")
    StressGrpc.StressBlockingStub blockingStub;

    @Test
    void largePayloadUnaryCallsSucceedWithEnlargedWindow() {
        GrpcStressSupport.runLargePayloadEcho(blockingStub, GrpcStressSupport.oneMegabytePayload());
    }
}
