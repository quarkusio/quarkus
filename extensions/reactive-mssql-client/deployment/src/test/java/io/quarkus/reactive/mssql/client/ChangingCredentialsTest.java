package io.quarkus.reactive.mssql.client;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.reactive.datasource.ChangingCredentialsTestBase;
import io.quarkus.test.QuarkusUnitTest;

public class ChangingCredentialsTest extends ChangingCredentialsTestBase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().withApplicationRoot(
            (jar) -> jar.addClass(ChangingCredentialsProvider.class).addClass(ChangingCredentialsTestResource.class)
                    .addAsResource("application-changing-credentials.properties", "application.properties"));

    public ChangingCredentialsTest() {
        super("dbo", "user2");
    }
}
