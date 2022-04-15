package io.quarkus.it.liquibase;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
public class LiquibaseFunctionalityNativeIT extends LiquibaseFunctionalityTest {

    // see: https://github.com/quarkusio/quarkus/issues/16292
    // if this is ever resolved, make sure to remove errorIfMissingOrEmpty="false" from includeAll in changeLog.xml
    @Override
    protected boolean isIncludeAllExpectedToWork() {
        return false;
    }
}
