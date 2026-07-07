package io.quarkus.gradle.application.internal.planning;

import io.quarkus.gradle.application.model.QuarkusApplicationBuildDescriptor;
import io.quarkus.gradle.application.model.QuarkusApplicationImageDescriptor;

public record ImagePlan(QuarkusApplicationBuildDescriptor owner,
        QuarkusApplicationImageDescriptor image, boolean push, boolean orderedReplacement) {
}
