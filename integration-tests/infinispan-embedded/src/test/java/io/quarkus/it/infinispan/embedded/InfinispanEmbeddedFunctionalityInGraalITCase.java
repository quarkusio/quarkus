package io.quarkus.it.infinispan.embedded;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.NativeImageTest;

/**
 * @author William Burns
 */
@QuarkusTestResource(InfinispanEmbeddedTestResource.class)
@NativeImageTest
public class InfinispanEmbeddedFunctionalityInGraalITCase extends InfinispanEmbeddedFunctionalityTest {

}
