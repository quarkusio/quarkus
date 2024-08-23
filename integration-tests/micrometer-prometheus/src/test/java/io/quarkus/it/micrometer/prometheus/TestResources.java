package io.quarkus.it.micrometer.prometheus;

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;

@WithTestResource(value = H2DatabaseTestResource.class, restrictToAnnotatedClass = false)
public class TestResources {
}
