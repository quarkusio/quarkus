package io.quarkus.it.rest.client.wronghost;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(value = ExternalWrongHostTestResourceUsingHostnameVerifier.class, restrictToAnnotatedClass = true)
public class ExternalWrongHostUsingHostnameVerifierTestCase extends BaseExternalWrongHostTestCase {
}
