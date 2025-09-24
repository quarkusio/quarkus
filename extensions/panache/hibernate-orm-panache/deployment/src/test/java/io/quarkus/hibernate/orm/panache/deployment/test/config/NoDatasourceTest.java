package io.quarkus.hibernate.orm.panache.deployment.test.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.panache.deployment.test.MyEntity;
import io.quarkus.test.QuarkusUnitTest;

public class NoDatasourceTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class))
            // Ideally we would not add quarkus-jdbc-h2 to the classpath and there _really_ wouldn't be a datasource,
            // but that's inconvenient given our testing setup,
            // so we'll just disable the implicit datasource.
            .overrideConfigKey("quarkus.datasource.jdbc", "false")
            .assertException(t -> assertThat(t)
                    .hasMessageContainingAll(
                            "Persistence unit '<default>' defines entities [" + MyEntity.class.getName()
                                    + "], but its datasource '<default>' cannot be found",
                            "Datasource '<default>' is not configured.",
                            "To solve this, configure datasource '<default>'",
                            "Refer to https://quarkus.io/guides/datasource for guidance.",
                            "Alternatively, disable Hibernate ORM by setting 'quarkus.hibernate-orm.enabled=false', and the entities will be ignored"));

    @Test
    public void test() {
        Assertions.fail("Startup should have failed");
    }

}
