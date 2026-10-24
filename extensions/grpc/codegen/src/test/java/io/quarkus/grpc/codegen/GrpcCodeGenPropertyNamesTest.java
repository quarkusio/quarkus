package io.quarkus.grpc.codegen;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.prebuild.CodeGenException;

public class GrpcCodeGenPropertyNamesTest {

    @Test
    public void unescapedCoordinatesAreRejected() {
        CodeGenException exception = assertThrows(CodeGenException.class,
                () -> GrpcCodeGen.checkScanForProtoPropertyNames(List.of(
                        "quarkus.generate-code.grpc.scan-for-proto=io.envoyproxy.controlplane:api",
                        "quarkus.generate-code.grpc.scan-for-proto-include.\"io.envoyproxy.controlplane")));

        assertTrue(exception.getMessage().contains("scan-for-proto-include.\"io.envoyproxy.controlplane"),
                exception.getMessage());
        assertTrue(exception.getMessage().contains("\\:"), exception.getMessage());
    }

    @Test
    public void unescapedCoordinatesInAProfileAreRejected() {
        assertThrows(CodeGenException.class,
                () -> GrpcCodeGen.checkScanForProtoPropertyNames(List.of(
                        "%dev.quarkus.generate-code.grpc.scan-for-proto-exclude.\"com.acme")));
    }

    @Test
    public void escapedCoordinatesAreAccepted() {
        assertDoesNotThrow(() -> GrpcCodeGen.checkScanForProtoPropertyNames(List.of(
                "quarkus.generate-code.grpc.scan-for-proto-include.\"io.envoyproxy.controlplane:api\"",
                "quarkus.generate-code.grpc.scan-for-proto-exclude.\"io.envoyproxy.controlplane:api\"",
                "quarkus.generate-code.grpc.scan-for-proto=io.envoyproxy.controlplane:api",
                "quarkus.http.port")));
    }

    @Test
    public void propertiesFormatCutsTheKeyAtTheUnescapedColon() throws IOException {
        Properties properties = new Properties();
        properties.load(new StringReader(
                "quarkus.generate-code.grpc.scan-for-proto-include.\"io.envoyproxy.controlplane:api\"=google/api/http.proto\n"));

        assertTrue(properties.containsKey("quarkus.generate-code.grpc.scan-for-proto-include.\"io.envoyproxy.controlplane"),
                properties.keySet().toString());
        assertThrows(CodeGenException.class,
                () -> GrpcCodeGen.checkScanForProtoPropertyNames(properties.stringPropertyNames()));
    }
}
