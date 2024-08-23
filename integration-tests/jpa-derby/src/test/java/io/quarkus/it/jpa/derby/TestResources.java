package io.quarkus.it.jpa.derby;

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.derby.DerbyDatabaseTestResource;

@WithTestResource(value = DerbyDatabaseTestResource.class, restrictToAnnotatedClass = false)
public class TestResources {
}
