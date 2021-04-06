package io.quarkus.it.liquibase;

import io.quarkus.test.junit.NativeImageTest;

@NativeImageTest
public class LiquibaseFunctionalityNativeIT extends LiquibaseFunctionalityTest {

    @Override
    protected boolean isIncludeAllExpectedToWork() {
        return false;
    }
}
