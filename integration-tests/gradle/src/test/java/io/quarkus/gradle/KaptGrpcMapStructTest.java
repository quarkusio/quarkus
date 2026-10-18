package io.quarkus.gradle;

import java.io.File;

import org.junit.jupiter.api.Test;

/**
 * Verifies that kapt annotation processors (e.g. MapStruct) can resolve types from Quarkus-generated
 * sources (e.g. gRPC stubs). Previously, kaptGenerateStubsKotlin did not inherit the codegen source
 * dirs injected into compileKotlin, causing MapStruct to fail with an unresolved type error.
 */
public class KaptGrpcMapStructTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testKaptWithGrpcAndMapStruct() throws Exception {
        final File projectDir = getProjectDir("kotlin-kapt-grpc-mapstruct");
        runGradleWrapper(projectDir, "clean", "build");
    }
}
