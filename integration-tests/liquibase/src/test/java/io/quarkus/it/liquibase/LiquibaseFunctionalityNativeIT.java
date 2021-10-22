package io.quarkus.it.liquibase;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
public class LiquibaseFunctionalityNativeIT extends LiquibaseFunctionalityTest {

    @Override
    protected boolean isIncludeAllExpectedToWork() {
        return false;
    }
}
