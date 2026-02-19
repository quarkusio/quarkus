package io.quarkus.test.junit;

import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.CuratedApplication;

public record PrepareResult(AugmentAction augmentAction, CuratedApplication curatedApplication) {
}
