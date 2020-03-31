package io.quarkus.agroal.test;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.QuarkusUnitTest;

public class CompatibleKindAndDriverDataSourceConfigTest {

    @Inject
    AgroalDataSource defaultDataSource;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("compatible-kind-and-driver-datasource.properties");

    @Test
    public void testApplicationStarts() {

    }
}
