package io.quarkus.it.panache

import io.quarkus.test.common.WithTestResource
import io.quarkus.test.h2.H2DatabaseTestResource

@WithTestResource(H2DatabaseTestResource::class, restrictToAnnotatedClass = false)
class TestResources
