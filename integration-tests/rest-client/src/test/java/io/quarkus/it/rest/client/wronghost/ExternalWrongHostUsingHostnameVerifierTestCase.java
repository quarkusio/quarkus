package io.quarkus.it.rest.client.wronghost;

import io.quarkus.it.rest.client.trustall.BadHostServiceTestResource;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@WithTestResource(BadHostServiceTestResource.class)
@WithTestResource(ExternalWrongHostTestResourceUsingHostnameVerifier.class)
public class ExternalWrongHostUsingHostnameVerifierTestCase extends BaseExternalWrongHostTestCase {
}
