package io.quarkus.it.rest.client.wronghost;

import io.quarkus.it.rest.client.trustall.BadHostServiceTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(value = BadHostServiceTestResource.class, restrictToAnnotatedClass = true)
@QuarkusTestResource(value = ExternalWrongHostTestResourceUsingHostnameVerifier.class, restrictToAnnotatedClass = true)
public class ExternalWrongHostUsingHostnameVerifierTestCase extends BaseExternalWrongHostTestCase {
}
