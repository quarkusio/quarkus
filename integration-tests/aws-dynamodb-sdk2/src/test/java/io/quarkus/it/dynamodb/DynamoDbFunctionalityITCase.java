package io.quarkus.it.dynamodb;

import io.quarkus.test.junit.SubstrateTest;

@SubstrateTest
public class DynamoDbFunctionalityITCase extends DynamoDbFunctionalityTest {

    @Override
    protected String asyncTable() {
        return "native-async";
    }

    @Override
    protected String blockingTable() {
        return "native-blocking";
    }
}
