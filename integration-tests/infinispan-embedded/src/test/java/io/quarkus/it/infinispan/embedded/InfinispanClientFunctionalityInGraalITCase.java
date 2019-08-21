package io.quarkus.it.infinispan.embedded;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.SubstrateTest;

/**
 * @author William Burns
 */
@QuarkusTestResource(InfinispanEmbeddedTestResource.class)
@SubstrateTest
public class InfinispanClientFunctionalityInGraalITCase extends InfinispanClientFunctionalityTest {

}
