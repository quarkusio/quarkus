package io.quarkus.it.rest.client.wronghost;

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@WithTestResource(ExternalWrongHostTestResourceUsingVerifyHost.class)
public class ExternalWrongHostUsingVerifyHostTestCase extends BaseExternalWrongHostTestCase {
}
