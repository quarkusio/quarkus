package io.quarkus.test.junit;

import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.CuratedApplication;

record PrepareResult(AugmentAction augmentAction, QuarkusTestProfile profileInstance,
        CuratedApplication curatedApplication) {
}
