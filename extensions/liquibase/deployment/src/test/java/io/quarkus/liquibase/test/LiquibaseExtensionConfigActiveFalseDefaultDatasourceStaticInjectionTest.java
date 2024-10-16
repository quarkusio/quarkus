package io.quarkus.liquibase.test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.liquibase.LiquibaseFactory;
import io.quarkus.test.QuarkusUnitTest;

public class LiquibaseExtensionConfigActiveFalseDefaultDatasourceStaticInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.datasource.active", "false");

    @Inject
    MyBean myBean;

    @Test
    @DisplayName("If the default datasource is deactivated, the application should boot, but Liquibase should be deactivated for that datasource")
    public void testBootSucceedsButLiquibaseDeactivated() {
        assertThatThrownBy(myBean::useLiquibase)
                .cause()
                .hasMessageContainingAll("Unable to find datasource '<default>' for Liquibase",
                        "Datasource '<default>' was deactivated through configuration properties.",
                        "To solve this, avoid accessing this datasource at runtime",
                        "Alternatively, activate the datasource by setting configuration property 'quarkus.datasource.active'"
                                + " to 'true' and configure datasource '<default>'",
                        "Refer to https://quarkus.io/guides/datasource for guidance.");
    }

    @ApplicationScoped
    public static class MyBean {
        @Inject
        LiquibaseFactory liquibase;

        public void useLiquibase() {
            liquibase.getConfiguration();
        }
    }

}
