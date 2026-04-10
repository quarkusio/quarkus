package io.quarkus.it.nat.annotation;

import io.quarkus.runtime.annotations.RegisterForReflection;

// Now UNSUPPORTED approach: Triggers warning during the native build.
// Ignored by GraalVM/Mandrel 25.0
@RegisterForReflection(serialization = true, lambdaCapturingTypes = "io.quarkus.it.nat.annotation.LegacyLambdaHolder$$Lambda*")
public class LegacyHints {
}
