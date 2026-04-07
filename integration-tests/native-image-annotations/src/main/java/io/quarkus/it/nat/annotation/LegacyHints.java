package io.quarkus.it.nat.annotation;

import io.quarkus.runtime.annotations.RegisterForReflection;

// 2. The legacy approach: This triggers your log.warnf during the native build
// and will be ignored by GraalVM 25.0
@RegisterForReflection(serialization = true, lambdaCapturingTypes = "io.quarkus.it.nat.annotation.LegacyLambdaHolder$$Lambda*")
public class LegacyHints {
}
